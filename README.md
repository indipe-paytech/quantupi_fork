

Requires merchant payment request url for receiver!

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter development, view the
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

# quantupi

## [![version](https://img.shields.io/pub/v/quantupi)](https://pub.dev/packages/quantupi)

complete the payments using installed UPI/ Banking payment apps.

Package implements [UPI Deep Linking And Proximity Integration Specification]().

- Package supports 
    - Android | iOS platforms
    - amazonpay
    - bhimupi
    - googlepay
    - mipay
    - mobikwik
    - myairtelupi
    - paytm
    - phonepe
    - sbiupi 

## Big Limitation For iOS

- The custom schemes mechanism has no way to return a transaction status to your calling code. The calling code can only know if a UPI payment app was launched successfully or not. Thus in iOS there is no way to know the transaction id by the app which calls for payment.

## Getting Started

Add this package to your flutter project's `pubspec.yaml` as a dependency as follows:

```yaml
dependencies:
  ...
  quantupi: ^0.0.4
```

Import the package as follows:

```dart
import 'package:quantupi/quantupi.dart';
```

### iOS configuration

In `Runner/Info.plist` add or modify the `LSApplicationQueriesSchemes` key so it includes custom query schemes shown as follows:

```xml
  <key>LSApplicationQueriesSchemes</key>
  <array>
    ...
    <string>amazonpay</string>
    <string>bhimupi</string>
    <string>gpay</string>
    <string>mipay</string>
    <string>mobikwik</string>
    <string>myairtelupi</string>
    <string>paytm</string>
    <string>phonepe</string>
    <string>upi</string>
    <string>upibillpay</string>
    <string>sbiupi</string>
    <string>whatsapp</string>
    ...
  </array>
```

### Usage

#### Create an instance of Quantupi (which requires receiverUpiId, receiverName, transactionRefId, transactionNote, amount)

```dart
Quantupi upi = Quantupi(
      receiverUpiId: 'merchant737120.augp@aubank',
      receiverName: 'Tester',
      transactionRefId: 'TestingId',
      transactionNote: 'Not actual. Just an example.',
      amount: 1.0,
    );
```

#### Initiate payment

```dart
final response = await upi.startTransaction();
print(response);
```

## Behaviour, Limitations & Measures

### Android

#### Flow

- On Android, the [UPI Deep Linking And Proximity Integration Specification]() is implemented using Intents.
- An Intent call with transaction request parameters includes the specific UPI app to be invoked.
- The plugin layer parses this response to create a `UpiTransactionResponse` object that is returned to your calling code. This object clearly indicates the status of the UPI payment, i.e. was it successful, failed or being processed.

#### Measures

It is advised that you implement a server-side payment verification on top of this status reporting, to avoid being affected by any hacks in the UPI transaction workflow on client's phone.

### iOS

#### Flow

- On iOS, the [UPI Deep Linking And Proximity Integration Specification]() is implemented using iOS custom schemes.
- Each UPI payment app can listen to a payment request of the form `upi://pay?...` sent by a caller app to iOS.
- The specification does not let you specify the target app's identifier in this request. On iOS, there is no other disambiguation measure available such as any ordering of the UPI payment apps that can be retrieved using any iOS APIs. Hence, it's impossible to know which UPI payment app will be invoked.
- One of the applicable apps gets invoked and it processes the payment. The custom schemes mechanism has no way to return a transaction status to your calling code. The calling code can only know if a UPI payment app was launched successfully or not.

#### Measures

- You will have to implement a payment verification on top of functionality available through this package.
- You should distinguish discovered and supported-only apps using the mechanism in the above section. The example app can be used for reference.

## UPI Apps' Functional Status Dynamics

UPI standards and systems are evolving, and accordingly behaviour and functionality of the UPI payment apps are changing.

### Support for merchant and non-merchant payments

The [UPI Deep Linking And Proximity Integration Specification]() is designed for merchant payments. It includes parameters in payment request that can be provided only if the payment was made to a merchant e.g. the merchant code (`mc` parameter), and a signature (crypto-hash) of the request created using merchant's private key.

However; various applications have been accepting requests without merchant details and signature and successfully processing payments. Possibly, the dilution could be due to the reason that such a package can only automate filling a payment form, and unless the user verifies the details in the form and enters the UPI pin, no damaging payments can be really made.

Over last few months, few applications have started changing their behaviour around non-merchant payments and one or more of the following are seen in few apps:

- An implicit "unverified source" warning or a direct warning indicating that merchant data in the request is not correct
- Z7 error, "Transaction Frequency Limit Exceeded": See [UPI Error and Response Codes]()
- U16 error, "Risk Threshold Exceeded", see [UPI Error and Response Codes]()
- An implicit "Security error"

This behaviour sometimes is not consistent even for the same app. For example, WhatsApp successfully completes transactions on iOS; but rejects non-merchant transactions on Android.

If you believe that your app's users' money is secure via their UPI pin, and you can let them use apps that successfully complete non-merchant transactions, then go ahead and pick the working apps in [Apps]() and integrate this package.

### Regressions

It's seen that post the Bank mergers of 2020-21 some of the bank apps have stopped working, even though they are still in Play Store and/or App Store.

### iOS minimum versions

Several BHIM apps have stopped working on <iOS 13.5. This package's iOS support is verified on iPhone with iOS 14+.

### Love to experiment yourself?

Please see API documentation. To support this experimentation, we would add further tweaking to allow you to access UPI apps not seen and supported by this package shortly on Android.

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

This project follows the all-contributors specification. Contributions of any kind welcome and will be recognised!
