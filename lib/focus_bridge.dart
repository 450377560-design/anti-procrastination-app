import 'package:flutter/services.dart';

class FocusBridge {
  static const MethodChannel _ch = MethodChannel('focus_service');

  static void init({void Function()? onInterruption}) {
    _onInterruption = onInterruption;
    _ch.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onInterruption':
          _onInterruption?.call();
          break;
      }
      return;
    });
  }

  static void Function()? _onInterruption;

  static Future<void> start({int minutes = 25, bool lock = true}) =>
      _ch.invokeMethod('startFocus', {'minutes': minutes, 'lock': lock});

  static Future<void> stop() => _ch.invokeMethod('stopFocus');

  static Future<void> plannerEnable({int hour = 21, int minute = 30}) =>
      _ch.invokeMethod('plannerEnable', {'hour': hour, 'minute': minute});

  static Future<void> plannerDisable() => _ch.invokeMethod('plannerDisable');
}
