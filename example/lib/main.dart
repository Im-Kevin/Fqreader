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

        double bodyHeight = (ScreenUtil.screenHeight - ScreenUtil.appBarHeight);
        Size viewSize = Size(ScreenUtil.screenWidth, ScreenUtil.screenHeight);
        Rect scanRect = Rect.fromLTWH(
            ScreenUtil.screenWidth * 0.1,
            (bodyHeight - bodyHeight * 0.8) / 2 + 60,
            ScreenUtil.screenWidth * 0.8,
            ScreenUtil.screenWidth * 0.8);
        return new Scaffold(
          appBar: new AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Stack(
            children: <Widget>[
              Positioned(
                top: 0.0,
                left: 0.0,
                child: FlatButton(
                  child: Text("启动扫描"),
                  color: Colors.red,
                  onPressed: () => scanView.currentState.startScan(),
                ),
              ),
              Positioned(
                top: 0.0,
                left: 80.0,
                child: FlatButton(
                  child: Text("暂停扫描"),
                  color: Colors.red,
                  onPressed: () => scanView.currentState.stopScan(),
                ),
              ),
              Positioned(
                top: 0.0,
                left: 160.0,
                child: FlatButton(
                  child: Text("开灯"),
                  color: Colors.red,
                  onPressed: () => scanView.currentState.turnOn(),
                ),
              ),
              Positioned(
                top: 0.0,
                left: 240.0,
                child: FlatButton(
                  child: Text("关灯"),
                  color: Colors.red,
                  onPressed: () => scanView.currentState.turnOff(),
                ),
              ),
              Positioned(
                top: 60.0,
                left: 0.0,
                child: FlatButton(
                  child: Text("扫描图片"),
                  color: Colors.red,
                  onPressed: () async {
                    var image = await ImagePicker.pickImage(source: ImageSource.camera);
                    var result = await Fqreader.decodeImg(image, [ScanType.ALL]);
                    if(result == null){
                      showWeuiSuccessToast(
                          context: context, message: Text("未扫描到数据"),closeDuration:Duration(milliseconds: 3000));
                    }else{
                      showWeuiSuccessToast(
                          context: context, message: Text("扫描成功:" + result.data),closeDuration:Duration(milliseconds: 500));
                    }
                  },
                ),
              ),
              Positioned(
                top: scanRect.top - 60,
                left: scanRect.left,
                child: Container(
                  width: scanRect.width,
                  height: scanRect.height,
                  decoration: BoxDecoration(border: Border.all()),
                ),
              )
            ],
          ),
        );
      }),
    );
  }
}
