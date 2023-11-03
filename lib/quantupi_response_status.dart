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
