import 'dart:io';

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:quantupi/quantupi.dart';
import 'package:quantupi/quantupi_payment_apps.dart';
import 'package:quantupi/upi_app_metadata.dart';
import 'package:quantupi/upi_applications.dart';

void main() => runApp(const MyApp());

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String data = 'Testing plugin';

  String appname = paymentappoptions[0];
  late Quantupi upi;
  List<String>? packNames = [
    UpiApplication.bhim.androidPackageName,
    UpiApplication.phonePe.androidPackageName,
    UpiApplication.paytm.androidPackageName,
    UpiApplication.phonePeSimulator.androidPackageName,
  ];
  @override
  void initState() {
    upi = Quantupi();
    // getApps();
    super.initState();
  }

  getApps() async {

    await upi.getFilteredUpiApps(packNames);
  }

  Future<String> initiateTransaction(String? upiApp) async {
    String response = await upi.startTransaction(upiApp!,
        'upi://pay?pa=kawaldeepsingh25@oksbi&pn=Kawaldeep%20Singh&cu=INR&am=1&tn=testMoney');

    return response;
  }

  @override
  Widget build(BuildContext context) {
    bool isios = !kIsWeb && Platform.isIOS;
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              if (isios)
                DropdownButton<String>(
                  value: appname,
                  icon: const Icon(Icons.arrow_drop_down),
                  iconSize: 24,
                  elevation: 16,
                  underline: Container(
                    height: 0,
                    // color: ,
                  ),
                  onChanged: (String? newValue) {
                    setState(() {
                      appname = newValue!;
                    });
                  },
                  items: paymentappoptions
                      .map<DropdownMenuItem<String>>((String value) {
                    return DropdownMenuItem<String>(
                      value: value,
                      child: Container(
                        margin: const EdgeInsets.symmetric(
                          horizontal: 20,
                        ),
                        child: Center(
                          child: Text(
                            value,
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              if (isios) const SizedBox(height: 20),
              Expanded(
                child: FutureBuilder<List<UpiAppMetaData>>(
                    future: upi.getFilteredUpiApps(packNames),
                    builder: (context, snapshot) {
                      if (snapshot.hasData) {
                        return GridView.builder(
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 3,
                            crossAxisSpacing: 8.0,
                            mainAxisSpacing: 8.0,
                          ),
                          itemCount: snapshot.data!.length,
                          itemBuilder: (context, index) {
                            return GestureDetector(
                              onTap: () async {
                                await initiateTransaction(
                                    snapshot.data![index].app);
                              },
                              child: Card(
                                child: Padding(
                                  padding: const EdgeInsets.all(8.0),
                                  child: Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    crossAxisAlignment:
                                        CrossAxisAlignment.center,
                                    children: [
                                      Image.memory(
                                        snapshot.data![index].icon!,
                                        width: 40,
                                        height: 40,
                                      ),
                                      const SizedBox(height: 10),
                                      Text(
                                        snapshot.data![index].name!,
                                        style: const TextStyle(
                                          fontSize: 14,
                                          fontWeight: FontWeight.bold,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            );
                          },
                        );
                      }
                      return Container();
                    }),
              ),
              // ElevatedButton(
              //   onPressed: () async {
              //     // String value = await initiateTransaction(
              //     //   app: isios ? appoptiontoenum(appname) : null,
              //     // );
              //     // setState(() {
              //     //   data = value;
              //     // });
              //   },
              //   style: ElevatedButton.styleFrom(
              //     padding: const EdgeInsets.symmetric(
              //       horizontal: 30,
              //       vertical: 15,
              //     ),
              //   ),
              //   child: const Text(
              //     "Tap to pay",
              //     style: TextStyle(fontSize: 20),
              //   ),
              // ),
              const SizedBox(
                height: 20,
              ),
              Padding(
                padding: const EdgeInsets.all(20.0),
                child: Text(
                  data,
                  style: const TextStyle(fontSize: 20),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }

  QuantUPIPaymentApps appoptiontoenum(String appname) {
    switch (appname) {
      case 'Amazon Pay':
        return QuantUPIPaymentApps.amazonpay;
      case 'BHIMUPI':
        return QuantUPIPaymentApps.bhimupi;
      case 'Google Pay':
        return QuantUPIPaymentApps.googlepay;
      case 'Mi Pay':
        return QuantUPIPaymentApps.mipay;
      case 'Mobikwik':
        return QuantUPIPaymentApps.mobikwik;
      case 'Airtel Thanks':
        return QuantUPIPaymentApps.myairtelupi;
      case 'Paytm':
        return QuantUPIPaymentApps.paytm;

      case 'PhonePe':
        return QuantUPIPaymentApps.phonepe;
      case 'SBI PAY':
        return QuantUPIPaymentApps.sbiupi;
      default:
        return QuantUPIPaymentApps.googlepay;
    }
  }
}

const List<String> paymentappoptions = [
  'Amazon Pay',
  'BHIMUPI',
  'Google Pay',
  'Mi Pay',
  'Mobikwik',
  'Airtel Thanks',
  'Paytm',
  'PhonePe',
  'SBI PAY',
];
