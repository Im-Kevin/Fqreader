import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fqreader/fqreader.dart';
import 'package:flustars/flustars.dart';
import 'package:cool_ui/cool_ui.dart';
import 'package:image_picker/image_picker.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  GlobalKey<ScanViewState> scanView;

  @override
  void initState() {
    super.initState();
    scanView = GlobalKey<ScanViewState>();
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: Builder(builder: (context) {
        ScreenUtil.getInstance().init(context);
        Size pictureSize = Size(ScreenUtil.screenWidth, ScreenUtil.screenHeight);
        Size viewSize = Size(ScreenUtil.screenWidth, 200);
        Size scanSize = Size(
            ScreenUtil.screenWidth * 0.8,
            ScreenUtil.screenWidth * 0.8);
        return new Scaffold(
          appBar: new AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Column(
            children: <Widget>[ ScanView(
              key: scanView,
                  scanSize: scanSize,
                viewSize: viewSize,
                pictureSize: pictureSize,
              devicePixelRatio:ScreenUtil.screenDensity,
                onScan: (result) async{
                  print(result.data);
                  return false;
                },
              ),
              FlatButton(child: Text('启动'),onPressed :()=>scanView.currentState.startScan()),
              FlatButton(child: Text('关闭'),onPressed :()=>scanView.currentState.stopScan()),
              FlatButton(child: Text('开灯'),onPressed :()=>scanView.currentState.turnOn()),
              FlatButton(child: Text('关灯'),onPressed :()=>scanView.currentState.turnOff()),
            ],
          )
        );
      }),
    );
  }
}
