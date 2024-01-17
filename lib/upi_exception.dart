import 'package:flutter/services.dart';

/// Exception class for handling UPI (Unified Payments Interface) related errors.
///
/// This class extends the built-in `Exception` class and provides specific
/// exception types for various UPI transaction scenarios.
class UpiException implements Exception {
  /// The type of UPI exception.
  final UpiExceptionType type;

  /// A human-readable message providing more information about the exception.
  final String? message;

  /// Additional details related to the exception.
  final dynamic details;

  /// The stack trace associated with the exception.
  final String? stacktrace;

  /// Creates an instance of [UpiException].
  ///
  /// Parameters:
  /// - [type]: The type of UPI exception.
  /// - [message]: A human-readable message providing more information about the exception.
  /// - [details]: Additional details related to the exception.
  /// - [stacktrace]: The stack trace associated with the exception.
  UpiException({
    required this.type,
    required this.message,
    required this.details,
    required this.stacktrace,
  });

  /// Factory method to create an instance of [UpiException] from a [PlatformException].
  ///
  /// Parameters:
  /// - [exception]: The platform exception to convert to a UPI exception.
  factory UpiException.fromException(PlatformException exception) {
    UpiExceptionType type;

    switch (exception.code) {
      case 'Cancelled':
        type = UpiExceptionType.cancelledException;
        break;
      case 'Failure':
        type = UpiExceptionType.failedException;
        break;
      case 'Submitted':
        type = UpiExceptionType.submittedException;
        break;
      case 'noUpiAppFound':
        type = UpiExceptionType.appNotFoundException;
        break;
      case 'noFilteredAppFound':
        type = UpiExceptionType.filteredAppNotFoundException;
        break;
      default:
        type = UpiExceptionType.unknownException;
        break;
    }

    return UpiException(
      type: type,
      message: exception.message,
      details: exception.details,
      stacktrace: exception.stacktrace,
    );
  }
}

/// Enum representing different types of UPI exceptions.
enum UpiExceptionType {
  /// Transaction is cancelled by the user.
  cancelledException,

  /// Transaction failed.
  failedException,

  /// Transaction is in PENDING state. Money might get deducted from the user’s account
  /// but not yet deposited in the payee’s account.
  submittedException,

  /// App for UPI transaction not found.
  appNotFoundException,

  /// No Supported Apps for UPI transaction not found.
  filteredAppNotFoundException,

  /// Unknown exception during UPI transaction.
  unknownException,
}
