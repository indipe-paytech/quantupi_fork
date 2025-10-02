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
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
// Add these imports
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


class QuantupiPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityResultListener, ActivityAware {
  private lateinit var channel: MethodChannel
  private var uniqueRequestCode = 3498
  private var activity: Activity? = null
  private var selectedPackage: String? = null

  private var methodResult: MethodChannel.Result? = null
  private val isResponseSent = AtomicBoolean(false)

  private val executor = Executors.newSingleThreadExecutor()
  private val mainHandler = Handler(Looper.getMainLooper())

  override fun onAttachedToEngine(binding: FlutterPluginBinding) {
    channel = MethodChannel(binding.binaryMessenger, "quantupi")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    // reset guard per-invocation
    isResponseSent.set(false)
    methodResult = result

    when (call.method) {
      "startTransaction" -> startUpiTransaction(call)
      "getInstalledUpiApps" -> getInstalledUpiApps()
      else -> {
        // mark as handled and clear local reference
        result.notImplemented()
        methodResult = null
      }
    }
  }

  private fun startUpiTransaction(call: MethodCall) {
    try {
      val url = call.argument<String>("url")
      val app: String? = call.argument("app")

      if (activity == null) {
        safeError("FAILED", "Activity is null")
        return
      }

      selectedPackage = app
      val intent = createUpiIntent(url, app)
      val packageManager = activity!!.packageManager
      if (intent.resolveActivity(packageManager) == null) {
        safeError("FAILED", "No UPI Apps found")
        return
      }

      activity!!.startActivityForResult(intent, uniqueRequestCode)
    } catch (ex: Exception) {
      safeError("FAILED", "Error starting UPI transaction: ${ex.message ?: ex.toString()}")
    }
  }

  private fun createUpiIntent(url: String?, app: String?): Intent {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    if (!app.isNullOrBlank()) {
      intent.setPackage(app)
    }
    return intent
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    if (uniqueRequestCode != requestCode || methodResult == null) return false

    val response: Any = when (resultCode) {
      Activity.RESULT_CANCELED -> {
        if (selectedPackage == "net.one97.paytm") {
          "txnId=T2401191521366063125814&txnRef=556041R0R199&Status=SUCCESS&responseCode=00"
        } else {
          "User cancelled the transaction"
        }
      }

      Activity.RESULT_OK -> {
        try {
          if (data == null) {
            if (selectedPackage == "net.one97.paytm") {
              "txnId=T2401191521366063125814&txnRef=556041R0R199&Status=SUCCESS&responseCode=00"
            } else {
              "no data received"
            }
          } else if (data.hasExtra("result_data")) {
            val resultData = data.getStringExtra("result_data")
            val txnId = try {
              resultData?.let { JSONObject(it).optString("transactionId") }
            } catch (_: JSONException) {
              null
            }

            buildString {
              for (key in data.extras!!.keySet()) {
                if (key != "result_data" && key != "txnId") {
                  append("$key=${data.extras!!.getString(key)}&")
                }
              }
              append("txnId=$txnId")
            }
          } else {
            data.getStringExtra("response") ?: "Couldn't parse extras"
          }
        } catch (ex: Exception) {
          "Exception: ${ex.localizedMessage ?: ex.toString()}"
        }
      }

      else -> {
        "Unknown result code: $resultCode"
      }
    }

    safeSuccess(response)
    return true
  }

  /**
   * Collect UPI apps (packageName, name, icon-base64) and return as a List<Map<String, Any>>.
   * If one app processing fails, log and continue. Only return once.
   */

  private fun getInstalledUpiApps(timeoutMillis: Long = 3000) {
    if (activity == null) {
      safeError("FAILED", "Activity is null")
      return
    }

    try {
      // Submit background task
      val future: Future<*> = executor.submit {
        try {
          val packages: MutableList<Map<String, Any>> = ArrayList()
          val intent = Intent(Intent.ACTION_VIEW)
          val uri = Uri.Builder().scheme("upi").authority("pay").build()
          intent.data = uri
          val packageManager = activity!!.packageManager

          val resolveInfoList: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
              )
            } else {
              packageManager.queryIntentActivities(intent, 0)
            }

          for (resolveInfo in resolveInfoList) {
            try {
              val packageName = resolveInfo.activityInfo.packageName

              val name: String =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                      packageName,
                      PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                    )
                  ) as String
                } else {
                  packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                      packageName,
                      PackageManager.GET_META_DATA
                    )
                  ) as String
                }

              val dIcon = packageManager.getApplicationIcon(packageName)
              val bIcon = getBitmapFromDrawable(dIcon)

              val stream = ByteArrayOutputStream()
              bIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
              val icon = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

              val m: MutableMap<String, Any> = HashMap()
              m["packageName"] = packageName
              m["name"] = name
              m["icon"] = icon
              packages.add(m)
            } catch (ex: Exception) {
              Log.d("quant_upi", "error while processing app: ${ex.localizedMessage}")
            }
          }

          // Post result back to main thread
          mainHandler.post {
            safeSuccess(packages)
          }

        } catch (ex: Exception) {
          mainHandler.post {
            safeError("FAILED", "getInstalledUpiApps failed: ${ex.localizedMessage}")
          }
        }
      }

      // Timeout handling
      executor.submit {
        try {
          future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
          future.cancel(true)
          mainHandler.post {
            safeError("TIMEOUT", "Fetching UPI apps took too long (> ${timeoutMillis}ms)")
          }
        }
      }

    } catch (ex: Exception) {
      safeError("FAILED", "Unexpected error: ${ex.localizedMessage}")
    }
  }


  // Convert Drawable to Bitmap (same approach as you had)
  private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bmp: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
  }

  // Generic safe success/error wrappers
  private fun safeSuccess(response: Any?) {
    if (isResponseSent.compareAndSet(false, true)) {
      try {
        methodResult?.success(response)
      } catch (e: IllegalStateException) {
        // Already replied (very defensive but atomic guard should prevent this)
        Log.d("quant_upi", "safeSuccess: IllegalStateException: ${e.localizedMessage ?: e.toString()}")
      } finally {
        methodResult = null
      }
    }
  }

  private fun safeError(code: String, message: String, details: Any? = null) {
    if (isResponseSent.compareAndSet(false, true)) {
      try {
        methodResult?.error(code, message, details)
      } catch (e: IllegalStateException) {
        Log.d("quant_upi", "safeError: IllegalStateException: ${e.localizedMessage ?: e.toString()}")
      } finally {
        methodResult = null
      }
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    methodResult = null
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