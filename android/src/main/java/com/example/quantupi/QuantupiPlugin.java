package com.example.quantupi;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;


/** QuantupiPlugin */
public class QuantupiPlugin implements FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, ActivityAware {

    int uniqueRequestCode = 3498;
    MethodChannel.Result finalResult;
    boolean exception = false;
    Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "quantupi");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("startTransaction")) {
            finalResult = result;
            String url = call.argument("url");
            try {
                exception = false;
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                activity.startActivityForResult(intent, uniqueRequestCode);
            } catch (Exception ex) {
                exception = true;
                result.error("FAILED", "invalid_parameters", null);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }

    // On receiving the response.
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (uniqueRequestCode != requestCode) {
            return false;
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            // User cancelled the transaction.
            Log.d("NOTE: ", "Received NULL, User cancelled the transaction.");
            finalResult.success("User cancelled the transaction");
        }

        if (resultCode == Activity.RESULT_OK && finalResult != null) {
            if(data != null) {
                try {
                    String response = data.getStringExtra("response");
                    Log.d("UPI", "onActivityResult: response " + response);
                    finalResult.success(response);
                } catch (Exception ex) {
                    Log.d("Exception: ", ex.toString());
                    finalResult.success(ex.toString());
                }
            } else {
                // Failed to receive any response from the invoked activity.
                Log.d("NOTE: ", "Received data is null");
                finalResult.success("no data received");
            }
        }

        return true;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }
}
