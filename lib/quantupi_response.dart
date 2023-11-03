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
