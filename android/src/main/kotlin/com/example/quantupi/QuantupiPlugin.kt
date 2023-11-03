package com.example.quantupi

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener


/** QuantupiPlugin  */
class QuantupiPlugin : FlutterPlugin, MethodCallHandler, ActivityResultListener, ActivityAware {
  private var uniqueRequestCode = 3498
  private var finalResult: MethodChannel.Result? = null
  private var activity: Activity? = null
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
    val channel = MethodChannel(flutterPluginBinding.binaryMessenger, "quantupi")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      "startTransaction" -> startUpiTransaction(result, call)
      else -> result.notImplemented()
    }
  }

  private fun startUpiTransaction(
    result: MethodChannel.Result,
    call: MethodCall,
  ) {
    val url = call.argument<String>("url")

    /// Check if url is valid upi url
    if (url == null || !url.startsWith("upi://")) {
      result.error("FAILED", "invalid_parameters", null)
      return
    }

    try {
      /// Storing result like this is a bad idea since this can cause issues in case of multiple
      // transactions involving multiple UPI Apps, however in practice this is unlikely to happen.
      finalResult = result
      val intent = createUpiIntent(url)
      val packageManager: PackageManager = activity!!.packageManager
      if (intent.resolveActivity(packageManager) == null) {
        result.error("FAILED", "No UPI Apps found", null)
        return
      }

      activity!!.startActivityForResult(intent, uniqueRequestCode)
    } catch (ex: Exception) {
      result.error("FAILED", "invalid_parameters", null)
    }
  }

  private fun createUpiIntent(url: String?): Intent {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    return intent
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {}

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

  override fun onDetachedFromActivityForConfigChanges() {}
  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
  override fun onDetachedFromActivity() {}
}