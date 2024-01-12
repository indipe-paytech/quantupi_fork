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
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import java.io.ByteArrayOutputStream
import android.util.Base64

/** QuantupiPlugin  */
class QuantupiPlugin : FlutterPlugin, MethodCallHandler, ActivityResultListener, ActivityAware {
  private lateinit var channel : MethodChannel
  private var uniqueRequestCode = 3498
  private var finalResult: MethodChannel.Result? = null
  private var activity: Activity? = null
  private var hasResponded = false
  private var exception = false

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "quantupi")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
//    hasResponded = false
//    this.result = result
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
    val url = call.argument<String>("url")
    val app: String? = call.argument("app")

    /// Check if url is valid upi url
//    if (url == null || !url.startsWith("upi://")) {
//      result.error("FAILED", "invalid_parameters", null)
//      return
//    }

    try {
      /// Storing result like this is a bad idea since this can cause issues in case of multiple
      // transactions involving multiple UPI Apps, however in practice this is unlikely to happen.
      finalResult = result
      exception = false
      val intent = createUpiIntent(url,app)
      val packageManager: PackageManager = activity!!.packageManager
      if (intent.resolveActivity(packageManager) == null) {
        result.error("FAILED", "No UPI Apps found", null)
        return
      }
//      if (packageManager?.let { intent.resolveActivity(it) } == null) {
//        success("activity_unavailable")
//        return
//      }

      activity!!.startActivityForResult(intent, uniqueRequestCode)
    } catch (ex: Exception) {
//      Log.e("quant_upi", ex.toString())
//      success("failed_to_open_app")

//      var msg=ex.message
//      result.success("${msg}")
      exception = true

      result.error("FAILED", "invalid_parameters", null)
    }
  }

  private fun createUpiIntent(url: String?, app: String?): Intent {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    intent.setPackage(app)
    return intent
  }

  private fun getInstalledUpiApps(result: MethodChannel.Result) {
    val packages: MutableList<Map<String, Any>> = ArrayList()
    val intent = Intent(Intent.ACTION_VIEW)
    Log.d("quantupi_fork", "getInstalledUpiApps")
    val uriBuilder = Uri.Builder()
    uriBuilder.scheme("upi").authority("pay")
//    uriBuilder.appendQueryParameter("pa", "test@ybl")
//    uriBuilder.appendQueryParameter("pn", "Test")
//    uriBuilder.appendQueryParameter("tn", "Get All Apps")
//    uriBuilder.appendQueryParameter("am", "1.0")
//    uriBuilder.appendQueryParameter("cr", "INR")
    val uri = uriBuilder.build()
    intent.data = uri
    val packageManager = activity!!.packageManager
    val resolveInfoList:List<ResolveInfo>
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      resolveInfoList = packageManager
              .queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    }else{
      resolveInfoList = packageManager
              .queryIntentActivities(intent, 0);
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

      } catch (e: Exception) {
        e.printStackTrace()
        Log.e("quant_upi", e.toString())
        result.error("getInstalledUpiApps", "exception", e)
      }
    }

    result.success(packages)
  }
  private fun encodeToBase64(image: Bitmap): String? {
    val byteArrayOS = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOS)
    return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.NO_WRAP)
  }
  // It converts the Drawable to Bitmap. There are other inbuilt methods too.
  private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
    val bmp: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
  }

  private fun success(o: String) {
    if (!hasResponded) {
      hasResponded = true
      finalResult?.success(o)
    }
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
        // User cancelled the transaction.
        // This should be a failure with appropriate error code
        onReturnResultToFlutter("User cancelled the transaction")
        return true
      }

      Activity.RESULT_OK -> {
        if (data == null) {
          // Failed to receive any response from the invoked activity.
          // This should be a failure with appropriate error code
          onReturnResultToFlutter("no data received")
          return true
        }

        return try {
          val response = data.getStringExtra("response")
          onReturnResultToFlutter(response!!)
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

//  override fun onDetachedFromActivityForConfigChanges() {}
//  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
//  override fun onDetachedFromActivity() {}
  override fun onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity;
  }

  override fun onDetachedFromActivity() {
    activity = null;
  }
}