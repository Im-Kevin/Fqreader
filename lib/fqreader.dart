import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Fqreader {
  static const MethodChannel _channel =
  const MethodChannel('fqreader');

  static Future<int> initView({@required Rect scanRect}) async {
    final int textureId = await _channel.invokeMethod('initView',{
      "scanRect":{
        "left":scanRect.left.toInt(),
        "top":scanRect.top.toInt(),
        "width":scanRect.width.toInt(),
        "height":scanRect.height.toInt()
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

  const ScanView({this.onScan,@required this.scanRect});

  @override
  State<StatefulWidget> createState() =>ScanViewState();
}

class ScanViewState extends State<ScanView>{
  int _textureId;
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    Fqreader.initView(scanRect:widget.scanRect).then((textureId){
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