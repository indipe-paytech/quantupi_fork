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
