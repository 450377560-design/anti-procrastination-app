import 'package:flutter/material.dart';
import 'focus_bridge.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  FocusBridge.init(onInterruption: () {
    _InterruptionUI.lastContext?.let((ctx) {
      _InterruptionUI.show(ctx);
    });
  });
  runApp(const AntiProApp());
}

class AntiProApp extends StatelessWidget {
  const AntiProApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Anti Pro',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.teal),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int minutes = 25;
  bool lock = true;
  TimeOfDay plannerTime = const TimeOfDay(hour: 21, minute: 30);
  bool plannerOn = false;

  @override
  Widget build(BuildContext context) {
    _InterruptionUI.lastContext = context;
    return Scaffold(
      appBar: AppBar(title: const Text('Anti Pro - 专注')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text('专注设置', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          Row(children: [
            const Text('时长(分钟)：'),
            Expanded(
              child: Slider(
                value: minutes.toDouble(),
                min: 5, max: 180, divisions: 175,
                label: '$minutes',
                onChanged: (v) => setState(() => minutes = v.round()),
              ),
            ),
            Switch(value: lock, onChanged: (v) => setState(() => lock = v)),
            const Text('锁屏'),
          ]),
          Row(children: [
            ElevatedButton(
              onPressed: () => FocusBridge.start(minutes: minutes, lock: lock),
              child: const Text('开始专注'),
            ),
            const SizedBox(width: 12),
            OutlinedButton(
              onPressed: FocusBridge.stop,
              child: const Text('停止'),
            ),
          ]),
          const SizedBox(height: 24),
          const Text('一日之计在于昨晚', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          Row(children: [
            TextButton(
              onPressed: () async {
                final t = await showTimePicker(context: context, initialTime: plannerTime);
                if (t != null) setState(() => plannerTime = t);
              },
              child: Text('提醒时间：${plannerTime.format(context)}'),
            ),
            const SizedBox(width: 12),
            Switch(
              value: plannerOn,
              onChanged: (v) async {
                setState(() => plannerOn = v);
                if (v) {
                  await FocusBridge.plannerEnable(hour: plannerTime.hour, minute: plannerTime.minute);
                } else {
                  await FocusBridge.plannerDisable();
                }
              },
            ),
            Text(plannerOn ? '已开启' : '已关闭'),
          ]),
          const SizedBox(height: 8),
          const Text('每天睡前用1分钟整理明日清单；通知会在设定时间发送。'),
        ],
      ),
    );
  }
}

extension _LetExt<T> on T {
  R let<R>(R Function(T it) block) => block(this);
}

class _InterruptionUI {
  static BuildContext? lastContext;
  static Future<void> show(BuildContext context) async {
    final reasons = ['消息提醒','刷短视频','查看邮件','接电话/开会','生理需求','其他'];
    String? picked;
    await showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const ListTile(title: Text('本次专注被打断，原因是？')),
          for (final r in reasons)
            ListTile(title: Text(r), onTap: () { picked = r; Navigator.pop(ctx); }),
          const SizedBox(height: 8),
        ]),
      ),
    );
    if (picked != null) {
      debugPrint('Interruption reason: $picked'); // TODO: 持久化统计
    }
  }
}
