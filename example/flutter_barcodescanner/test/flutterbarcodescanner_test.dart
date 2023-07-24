import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutterbarcodescanner/flutterbarcodescanner.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutterbarcodescanner');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Flutterbarcodescanner.platformVersion, '42');
  });
}
