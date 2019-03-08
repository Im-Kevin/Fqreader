library fqreader;

import 'dart:async';

import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Fqreader {
  static const MethodChannel _channel =
  const MethodChannel('fqreader');

  static Future<int> initView({@required Rect viewRect,@required Rect scanRect,double devicePixelRatio}) async {
    final int textureId = await _channel.invokeMethod('initView',{
      "viewRect":{
        "left":(viewRect.left * devicePixelRatio).toInt(),
        "top":(viewRect.top* devicePixelRatio).toInt(),
        "right":(viewRect.right* devicePixelRatio).toInt(),
        "bottom":(viewRect.bottom* devicePixelRatio).toInt()
      },
      "scanRect":{
        "left":(scanRect.left* devicePixelRatio).toInt(),
        "top":(scanRect.top* devicePixelRatio).toInt(),
        "right":(scanRect.right* devicePixelRatio).toInt(),
        "bottom":(scanRect.bottom* devicePixelRatio).toInt(),
      }
    });
    return textureId;
  }
  static Future<String> startScan() async{
    await _channel.invokeMethod('startScan');
  }

  static Future stopScan() async{
    await _channel.invokeMethod('stopScan');
  }
}


class ScanView extends StatefulWidget{
  final ValueChanged<String> onScan;
  final Rect scanRect;
  final Rect viewRect;

  const ScanView({this.onScan,@required this.viewRect,@required this.scanRect});

  @override
  State<StatefulWidget> createState() =>ScanViewState();
}

class ScanViewState extends State<ScanView>{
  int _textureId;
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    MediaQueryData mediaQuery = MediaQueryData.fromWindow(ui.window);
    Fqreader.initView(viewRect: widget.viewRect,scanRect:widget.scanRect,devicePixelRatio:mediaQuery.devicePixelRatio).then((textureId){
      setState(() {
        _textureId = textureId;
      });
      new EventChannel('fqreader/qrcodeEvents$_textureId')
          .receiveBroadcastStream()
          .listen(_listener);
    });
  }

  @override
  Widget build(BuildContext context) {
    return _textureId != null
        ? new Texture(textureId: _textureId)
        : new Container();
  }

  Future<String> startScan() async{

  }

  void _listener(dynamic value) {
    if(widget != null)
      widget.onScan(value);
  }
}