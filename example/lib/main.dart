import 'package:cool_ui/cool_ui.dart';
import 'package:flustars/flustars.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:fqreader/fqreader.dart';
// import 'package:image_picker/image_picker.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  GlobalKey<ZBarViewState> scanView;
  Size cameraSize;

  @override
  void initState() {
    super.initState();
    scanView = GlobalKey<ZBarViewState>();
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: Builder(builder: (context) {
        Size pictureSize = Size(
            ScreenUtil.getInstance().screenWidth,
            ScreenUtil.getInstance().screenHeight -
                ScreenUtil.getInstance().appBarHeight -
                ScreenUtil.getInstance().statusBarHeight);
        if (ScreenUtil.getInstance().screenDensity == 1) {
          return Container();
        }
        return new Scaffold(
            appBar: new AppBar(
              title: const Text('Plugin example app'),
            ),
            body: Stack(
              children: <Widget>[
                // ScanView(
                //   key: scanView,
                //   scanAilgn: Alignment.center,
                //   scanSize: scanSize,
                //   viewSize: pictureSize,
                //   maskColor: Colors.white,
                //   devicePixelRatio: ScreenUtil.getInstance().screenDensity,
                //   onScan: (result) async {
                //     showWeuiSuccessToast(context: context, message: Text(result.data));
                //     print(result.data);
                //     return false;
                //   },
                // ),
                ZBarView(
                  key: scanView,
                  width: pictureSize.width.toInt(),
                  height: pictureSize.height.toInt(),
                  onScan: (result) async {
                    showWeuiSuccessToast(context: context, message: Text(result.data));
                    await Future.delayed(Duration(seconds: 1));
                    return true;
                  },
                ),
                Container(
                    child: Text(
                        '屏幕dp宽度:${ScreenUtil.getInstance().screenWidth}\n屏幕dp高度:${ScreenUtil.getInstance().screenHeight}\n DP 对 PX比率:${ScreenUtil.getInstance().screenDensity}\n' +
                            (this.cameraSize != null
                                ? '图片宽度:${cameraSize.width}\n图片高度:${cameraSize.height}'
                                : '')))
              ],
            ));
      }),
    );
  }
}
