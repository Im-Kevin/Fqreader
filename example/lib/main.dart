import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fqreader/fqreader.dart';
import 'package:flustars/flustars.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    return new MaterialApp(
      home: Builder(builder: (context){
        ScreenUtil.getInstance().init(context);

        double bodyHeight = (ScreenUtil.screenHeight - ScreenUtil.appBarHeight);
        Rect scanRect= Rect.fromLTWH(
            ScreenUtil.screenWidth * 0.1,
            (bodyHeight - ScreenUtil.screenWidth * 0.8) / 2,
            ScreenUtil.screenWidth * 0.8,
            ScreenUtil.screenWidth * 0.8);
        return new Scaffold(
          appBar: new AppBar(
            title: const Text('Plugin example app'),
          ),
          body: Stack(
            children: <Widget>[
              ScanView(onScan: (value){
                print(value);
              },scanRect: Rect.fromLTWH(
                  scanRect.left * MediaQuery.of(context).devicePixelRatio,
                  scanRect.top * MediaQuery.of(context).devicePixelRatio,
                  scanRect.width * MediaQuery.of(context).devicePixelRatio,
                  scanRect.height * MediaQuery.of(context).devicePixelRatio)),
              Positioned(
                top: 0.0,
                left: 0.0,
                child:FlatButton(child: Text("启动扫描"),onPressed: ()=>Fqreader.startScan(),),
              ),
              Positioned(
                top: 0.0,
                left: 80.0,
                child:FlatButton(child: Text("暂停扫描"),onPressed: ()=>Fqreader.stopScan(),),
              ),
              Positioned(
                top: scanRect.top,
                left: scanRect.left,
                child: Container(
                  width: scanRect.width,
                  height: scanRect.height,
                  decoration: BoxDecoration(
                      border: Border.all()
                  ),
                ),
              )
            ],
          ),
        );
      }),
    );
  }
}
