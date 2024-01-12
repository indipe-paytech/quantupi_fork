import 'dart:async';
import 'dart:io';
import 'dart:developer';
import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'upi_app_model.dart';
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

  Future<List<UpiApp>> getUpiApps() async {
    List<UpiApp>? upiList =
    await getAllUpiApps();
    if (upiList.isNotEmpty) {
      return upiList;
    }
    return [];
  }

  Future<List<UpiApp>> getAllUpiApps() async {
    final List<Map>? apps = await _channel.invokeListMethod<Map>('getInstalledUpiApps');
    List<UpiApp> upiIndiaApps = [];
    apps?.forEach((Map app) {
      var icon= app['icon'];
      if (app['packageName'] == "in.org.npci.upiapp" ||
          app['packageName'] == "com.phonepe.app" ||
          app['packageName'] == "net.one97.paytm" ||
          app['packageName'] == "com.phonepe.simulator"

      ///Coming soon
      ///app['packageName'] == "com.google.android.apps.nbu.paisa.user"
      ///app['packageName'] == "in.amazon.mShop.android.shopping"
      // app['packageName'] == "com.freecharge.android" ||
      // app['packageName'] == "com.axis.mobile" ||
      // app['packageName'] == "com.infrasofttech.centralbankupi" ||
      // app['packageName'] == "com.infra.boiupi" ||
      // app['packageName'] == "com.lcode.corpupi" ||
      // app['packageName'] == "com.lcode.csbupi" ||
      // app['packageName'] == "com.dbs.in.digitalbank" ||
      // app['packageName'] == "com.equitasbank.upi" ||
      // app['packageName'] == "com.mgs.hsbcupi" ||
      // app['packageName'] == "com.csam.icici.bank.imobile" ||
      // app['packageName'] == "com.lcode.smartz" ||
      // app['packageName'] == "com.mgs.induspsp" ||
      // app['packageName'] == "com.msf.kbank.mobile" ||
      // app['packageName'] == "com.hdfcbank.payzapp" ||
      // app['packageName'] == "com.Version1" ||
      // app['packageName'] == "com.psb.omniretail" ||
      // app['packageName'] == "com.rblbank.mobank" ||
      // app['packageName'] == "com.lcode.ucoupi" ||
      // app['packageName'] == "com.ultracash.payment.customer" ||
      // app['packageName'] == "com.YesBank" ||
      // app['packageName'] == "com.bankofbaroda.upi" ||
      // app['packageName'] == "com.myairtelapp" ||
      // app['packageName'] == "com.dreamplug.androidapp" ||
      // app['packageName'] == "com.sbi.upi"
      ) {
        // || app['packageName']== "com.whatsapp"
        // || app['packageName']== "com.whatsapp.w4b") {
        upiIndiaApps.add(UpiApp.fromMap(Map<String, dynamic>.from(app)));
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
