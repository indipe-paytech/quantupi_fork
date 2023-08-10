import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

class Quantupi {
  Quantupi({required this.url}) {
    _channel.setMethodCallHandler(_fromNative);
  }
  static const _channel = MethodChannel('quantupi');

  final String url;

  Future<void> _fromNative(MethodCall call) async {
    if (call.method == 'callTestResuls') {
      print('callTest result = ${call.arguments}');
    }
  }

  Future<String> startTransaction() async {
    try {
      if (Platform.isAndroid) {
        final String response =
            await _channel.invokeMethod('startTransaction', {
          'url': url,
        });
        return response;
      } else if (Platform.isIOS) {
        final result = await _channel.invokeMethod(
          'launch',
          {
            'uri': transactiondetailstostring(url: url),
          },
        );
        return result == true
            ? "Successfully Launched App!"
            : "Something went wrong!";
      } else {
        throw PlatformException(
          code: 'ERROR',
          message: 'Platform not supported!',
        );
      }
    } catch (error) {
      throw Exception(error);
    }
  }
}

String transactiondetailstostring({
  required String url,
}) {
  return url;
}

enum QuantUPIPaymentApps {
  amazonpay,
  bhimupi,
  googlepay,
  mipay,
  mobikwik,
  myairtelupi,
  paytm,
  phonepe,
  sbiupi,
}

class QuantupiResponse {
  String? transactionId;
  String? responseCode;
  String? approvalRefNo;

  /// DO NOT use the string directly. Instead use [QuantupiResponseStatus]
  String? status;
  String? transactionRefId;

  QuantupiResponse(String responseString) {
    List<String> parts = responseString.split('&');

    for (int i = 0; i < parts.length; ++i) {
      String key = parts[i].split('=')[0];
      String value = parts[i].split('=')[1];
      if (key.toLowerCase() == "txnid") {
        transactionId = value;
      } else if (key.toLowerCase() == "responsecode") {
        responseCode = value;
      } else if (key.toLowerCase() == "approvalrefno") {
        approvalRefNo = value;
      } else if (key.toLowerCase() == "status") {
        if (value.toLowerCase() == "success") {
          status = "success";
        } else if (value.toLowerCase().contains("fail")) {
          status = "failure";
        } else if (value.toLowerCase().contains("submit")) {
          status = "submitted";
        } else {
          status = "other";
        }
      } else if (key.toLowerCase() == "txnref") {
        transactionRefId = value;
      }
    }
  }
}

// This class is to match the status of transaction.
// It is advised to use this class to compare the status rather than doing string comparision.
class QuantupiResponseStatus {
  /// SUCCESS occurs when transaction completes successfully.
  static const String success = 'success';

  /// SUBMITTED occurs when transaction remains in pending state.
  static const String submitted = 'submitted';

  /// Deprecated! Don't use it. Use FAILURE instead.
  static const String failed = 'failure';

  /// FAILURE occurs when transaction fails or user cancels it in the middle.
  static const String failure = 'failure';

  /// In case status is not any of the three accepted value (by chance).
  static const String other = 'other';
}

// Class that contains error responses that must be used to check for errors.
class QuantupiResponseError {
  /// When user selects app to make transaction but the app is not installed.
  static const String appnotinstalled = 'app_not_installed';

  /// When the parameters of UPI request is/are invalid or app cannot proceed with the payment.
  static const String invalidparameter = 'invalid_parameters';

  /// Failed to receive any response from the invoked activity.
  static const String nullresponse = 'null_response';

  /// User cancelled the transaction.
  static const String usercanceled = 'user_canceled';
}
