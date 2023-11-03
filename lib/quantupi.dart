import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

/// TODO: move to platform channel interface
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
      if (error is! Exception) {
        throw Exception(error);
      }

      rethrow;
    }
  }
}

String transactiondetailstostring({
  required String url,
}) {
  return url;
}
