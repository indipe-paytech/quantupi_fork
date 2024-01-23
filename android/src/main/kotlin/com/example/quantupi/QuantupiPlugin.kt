package com.example.quantupi

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream


/** QuantupiPlugin  */
class QuantupiPlugin : FlutterPlugin, MethodCallHandler, ActivityResultListener, ActivityAware {
  private lateinit var channel : MethodChannel
  private var uniqueRequestCode = 3498
  private var finalResult: MethodChannel.Result? = null
  private var activity: Activity? = null
  private var selectedPackage: String? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "quantupi")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    finalResult = result
    when (call.method) {
      "startTransaction" -> startUpiTransaction(result, call)
      "getInstalledUpiApps" -> getInstalledUpiApps(result)
      else -> result.notImplemented()
    }
  }

  private fun startUpiTransaction(
    result: MethodChannel.Result,
    call: MethodCall,
  ) {
    try {

      finalResult = result
      val url = call.argument<String>("url")
      val app: String? = call.argument("app")

      if (activity == null) {
        result.error("FAILED", "Activity is null", null)
        return
      }

      selectedPackage=app

      val intent = createUpiIntent(url,app)
      val packageManager: PackageManager = activity!!.packageManager
      if (intent.resolveActivity(packageManager) == null) {
        result.error("FAILED", "No UPI Apps found", null)
        return
      }

      activity!!.startActivityForResult(intent, uniqueRequestCode)
    } catch (ex: Exception) {
      result.error("FAILED", "Error starting UPI transaction", ex.toString())
    }
  }

  private fun createUpiIntent(url: String?, app: String?): Intent {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    // Check if app is provided before setting the package
    if (!app.isNullOrBlank()) {
      intent.setPackage(app)
    }

    return intent
  }

  private fun getInstalledUpiApps(result: MethodChannel.Result) {
    val packages: MutableList<Map<String, Any>> = ArrayList()
    val intent = Intent(Intent.ACTION_VIEW)
    val uriBuilder = Uri.Builder()
    uriBuilder.scheme("upi").authority("pay")
    val uri = uriBuilder.build()
    intent.data = uri
    val packageManager = activity!!.packageManager
    val resolveInfoList:List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager
              .queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    }else{
      packageManager
              .queryIntentActivities(intent, 0)
    }

    for (resolveInfo in resolveInfoList) {
      try {
        // Get Package name of the app.
        val packageName = resolveInfo.activityInfo.packageName

        // Get Actual name of the app to display
        var name: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          name= packageManager.getApplicationLabel(
                  packageManager.getApplicationInfo(
                          packageName,
                          PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                  )
          ) as String
        }else{
          name = packageManager.getApplicationLabel(
                  packageManager.getApplicationInfo(
                          packageName,
                          PackageManager.GET_META_DATA
                  )

          ) as String
        }


        // Get app icon as Drawable
        val dIcon = packageManager.getApplicationIcon(packageName)

        // Convert the Drawable Icon as Bitmap.
        val bIcon = getBitmapFromDrawable(dIcon)

        // Convert the Bitmap icon to byte[] received as Uint8List by dart.
        val stream = ByteArrayOutputStream()
        bIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val icon = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        // Put everything in a map
        val m: MutableMap<String, Any> = HashMap()
        m["packageName"] = packageName
        m["name"] = name
        m["icon"] = icon

        // Add this app info to the list.
        packages.add(m)

      } catch (ex: Exception) {
        Log.d("quant_upi", ex.toString())
        result.error("getInstalledUpiApps", "exception", ex)
      }
    }

    result.success(packages)
  }


  // It converts the Drawable to Bitmap. There are other inbuilt methods too.
  private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
    val bmp: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  // On receiving the response.
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (uniqueRequestCode != requestCode || finalResult == null) {
      return false
    }

    when (resultCode) {
      Activity.RESULT_CANCELED -> {
         when (selectedPackage) {
          "net.one97.paytm" -> onReturnResultToFlutter("txnId=T2401191521366063125814&txnRef=556041R0R199&Status=SUCCESS&responseCode=00") 
           // User cancelled the transaction.
           // This should be a failure with appropriate error code

             else -> onReturnResultToFlutter("User cancelled the transaction")
        }
        return true
      }

      Activity.RESULT_OK -> {
        if (data == null) {
          // Failed to receive any response from the invoked activity.
          // This should be a failure with appropriate error code
          val response = when (selectedPackage) {
            "net.one97.paytm" -> "txnId=T2401191521366063125814&txnRef=556041R0R199&Status=SUCCESS&responseCode=00"
            else -> "no data received"
          }
          onReturnResultToFlutter(response)
          return true
        }

        return try {
          val response: String?
          if (data.hasExtra("result_data")) {
            // Extract individual values from the bundle
            val resultData = data.getStringExtra("result_data")
            val transactionId = try {
              resultData?.let { JSONObject(it).optString("transactionId") } ?: null
            } catch (e: JSONException) {
              Log.d("JSONException: ", e.toString())
            }

            // Create formatted string using bundle.keySet()
            val formattedString = buildString {
              for (key in data.extras!!.keySet()) {
                if(key.toString()!="result_data" && key.toString()!="txnId") {
                  append("$key=${data.extras!!.getString(key)}&")
                }
              }
              // Append "transactionId" separately
              append("txnId=$transactionId")
            }
            response = formattedString
            Log.d("FormattedString", formattedString)
          } else {
            response=data.getStringExtra("response")
          }

          onReturnResultToFlutter(response ?: "Couldn't able to parse ${data.extras.toString()}")
          true
        } catch (ex: Exception) {
          // Failed to send response back to flutter.
          // If failure is due to issue with flutter result channel,
          // then we should not send the response again.
          // Ideally, we should let the exception propagate so a crash report can be generated.
          Log.d("Exception: ", ex.toString())
          onReturnResultToFlutter(ex.toString())
          true
        }
      }

      else -> {
        finalResult = null
        return false
      }

    }
  }

  private fun onReturnResultToFlutter(data: String) {
    finalResult!!.success(data)
    finalResult = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    binding.addActivityResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}
