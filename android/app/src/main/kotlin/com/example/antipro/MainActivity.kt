package com.example.antipro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "focus_service"
    private var methodChannel: MethodChannel? = null
    private var interruptionReceiver: BroadcastReceiver? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val ch = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel = ch

        ch.setMethodCallHandler { call, result ->
            when (call.method) {
                // 启动前台服务；可选同时打开锁屏界面
                "startFocus" -> {
                    val minutes = (call.argument<Int>("minutes") ?: 25)
                    val lock = (call.argument<Boolean>("lock") ?: false)

                    val svc = Intent(this, FocusForegroundService::class.java).apply {
                        putExtra("minutes", minutes)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)

                    if (lock) {
                        val lockIntent = Intent(this, FocusLockActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(lockIntent)
                    }
                    result.success(true)
                }

                // 通过向服务发送 STOP action 停止专注并回到 App（服务里会拉起 App）
                "stopFocus" -> {
                    val stop = Intent(this, FocusForegroundService::class.java)
                        .setAction(FocusForegroundService.ACTION_STOP)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(stop) else startService(stop)
                    result.success(true)
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 监听服务发来的“打断”广播 -> 通过 MethodChannel 回传 Flutter
        val filter = IntentFilter(FocusForegroundService.ACTION_INTERRUPTED_BROADCAST)
        interruptionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                methodChannel?.invokeMethod("onInterruption", null)
            }
        }
        registerReceiver(interruptionReceiver, filter)
    }

    override fun onDestroy() {
        try { unregisterReceiver(interruptionReceiver) } catch (_: Exception) {}
        interruptionReceiver = null
        super.onDestroy()
    }
}
