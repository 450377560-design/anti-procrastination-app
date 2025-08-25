import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';

class FocusBridge {
  static const MethodChannel _ch = MethodChannel('focus_service');

  /// 初始化：注册原生→Flutter回调
  static void init({void Function()? onInterruption}) {
    _onInterruption = onInterruption;
    _ch.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'onInterruption':
          _onInterruption?.call();
          break;
        // 预留更多事件：onTick/onFinish/onStop ...
        default:
          break;
      }
      return;
    });
  }

  static void Function()? _onInterruption;

  /// 启动前台专注模式
  static Future<void> start({int minutes = 25, bool lock = true}) {
    return _ch.invokeMethod('startFocus', {
      'minutes': minutes,
      'lock': lock,
    });
  }

  /// 停止前台专注模式
  static Future<void> stop() {
    return _ch.invokeMethod('stopFocus');
  }
}
