import 'dart:async';

import 'package:flutter/services.dart';

class Flutterbarcodescanner {
  static const MethodChannel _channel =
      const MethodChannel('flutterbarcodescanner');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
