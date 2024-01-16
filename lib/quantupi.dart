import 'dart:async';
import 'dart:io';
import 'dart:developer';
import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'upi_app_metadata.dart';
import 'upi_app_bundle.dart';
/// TODO: move to platform channel interface
class Quantupi {
  Quantupi() {
    _channel.setMethodCallHandler(_fromNative);
  }
  static const _channel = MethodChannel('quantupi');

  Future<void> _fromNative(MethodCall call) async {
    if (call.method == 'callTestResuls') {
      print('callTest result = ${call.arguments}');
    }
  }

  Future<String> startTransaction( String packageName,
       String url,) async {
    try {
      if (Platform.isAndroid) {
        final String response = await _channel
            .invokeMethod('startTransaction', {'url': url, 'app': packageName});
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

  Future<List<UpiAppMetaData>> getFilteredUpiApps(List<String>? listPackageNames) async {
    List<UpiAppMetaData>? upiList =
    await _getAllUpiApps(listPackageNames);

    if (upiList.isNotEmpty) {
      return upiList;
    }
    return [];
  }

  Future<List<UpiAppMetaData>> _getAllUpiApps(List<String>? listPackageNames) async {
    final List<Map>? apps = await _channel.invokeListMethod<Map>('getInstalledUpiApps');
    List<UpiAppMetaData> upiIndiaApps = [];

    apps?.forEach((Map app) {
      if (listPackageNames?.contains(app['packageName']) ?? false) {
        upiIndiaApps.add(UpiAppMetaData.fromMap(Map<String, dynamic>.from(app)));
      }
    });

    return upiIndiaApps;
  }

  /// Navigates to the App Store to download the specified UPI payment app.
  ///
  /// Parameters:
  /// - [url]: The App Store URL for the UPI payment app.
  ///
  /// Returns:
  /// A [Future] that completes after navigating to the App Store.
  Future<void> navigateToAppstore(String? url) async {
    try {
      await _channel.invokeMethod(
        'navigateToAppstore',
        {
          'uri': url,
        },
      );
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
