library fqreader;

import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


typedef ScanEvent = Future<bool> Function(String value);

class Fqreader {
  static const MethodChannel _channel =
  const MethodChannel('fqreader');

  static Future<String> decodeImg(File file,List<ScanType> scanType) async{
    var scanStr = new List<String>();
    scanType.forEach((item){
      scanStr.add(item.toString());
    });

    List<int> data = file.readAsBytesSync();
    Uint8List uData = new Uint8List.fromList(data);

    return await  _channel.invokeMethod('decodeImg',{
      "image": uData,
      "scanType": scanStr
    });
  }

  static Future<int> _initView({
      @required Size viewSize,
      @required Rect scanRect,
      @required List<ScanType> scanType,
      double devicePixelRatio
  }) async {
    var scanStr = new List<String>();
    scanType.forEach((item){
      scanStr.add(item.toString());
    });

    final int textureId = await _channel.invokeMethod('initView',{
      "viewRect":{
        "width":(viewSize.width* devicePixelRatio).toInt(),
        "height":(viewSize.height* devicePixelRatio).toInt(),
      },
      "scanRect":{
        "left":(scanRect.left* devicePixelRatio).toInt(),
        "top":(scanRect.top* devicePixelRatio).toInt(),
        "right":(scanRect.right* devicePixelRatio).toInt(),
        "bottom":(scanRect.bottom* devicePixelRatio).toInt(),
      },
      "scanType": scanStr
    });
    return textureId;
  }
  static Future _startScan() async{
    await _channel.invokeMethod('startScan');
  }

  static Future _stopScan() async{
    await _channel.invokeMethod('stopScan');
  }
  static Future _turnOn() async{
    await _channel.invokeMethod("turnOn");
  }
  static Future _turnOff() async{
    await _channel.invokeMethod("turnOff");
  }
  static Future _release() async{
    await _channel.invokeMethod("release");
  }
}


class ScanView extends StatefulWidget{
  /**
   * 扫描事件
   */
  final ScanEvent onScan;

  /**
   * 扫描区域大小
   */
  final Rect scanRect;

  /**
   * ScanView控件大小
   */
  final Size viewSize;

  /**
   * 是否立即扫描
   */
  final bool autoScan;

  /**
   * 是否连续扫描
   */
  final bool continuityScan;

  /**
   * 连续扫描间隔
   */
  final Duration scanInterval;

  /**
   *  扫描的条码类型
   */
  final List<ScanType> scanType;

  const ScanView({
      Key key,
      this.onScan,
      @required this.viewSize,
      @required this.scanRect,
      this.scanType = const [ScanType.ALL],
      this.autoScan = true,
      this.continuityScan = false,
      this.scanInterval = const Duration(milliseconds:500)})
    : super(key:key);

  @override
  State<StatefulWidget> createState() =>ScanViewState();
}

class ScanViewState extends State<ScanView>{
  int _textureId;
  StreamSubscription _readySubscription;
  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    MediaQueryData mediaQuery = MediaQueryData.fromWindow(ui.window);
    Fqreader._initView(
        viewSize: widget.viewSize,
        scanRect:widget.scanRect,
        devicePixelRatio:mediaQuery.devicePixelRatio,
        scanType: widget.scanType
    ).then((textureId){
      setState(() {
        _textureId = textureId;
      });
      if(widget.autoScan){
        Fqreader._startScan();
      }
      _readySubscription = new EventChannel('fqreader/scanEvents$_textureId')
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

  @override
  void dispose(){
    super.dispose();
    _readySubscription.cancel();
    Fqreader._release();
  }

  /**
   * 开始扫描
   */
  Future startScan() async{
    await Fqreader._startScan();
  }

  /**
   * 暂停扫描
   */
  Future stopScan() async{
    await Fqreader._stopScan();
  }
  /**
   * 开灯
   */
  Future turnOn() async{
    await Fqreader._turnOn();
  }
  /**
   * 关灯
   */
  Future turnOff() async{
    await Fqreader._turnOff();
  }

  void _listener(dynamic value) {
    if(widget != null)
      {
        if(!widget.continuityScan) //是否连续扫描
          {
            Fqreader._stopScan();
          }
        widget.onScan(value).then((result){
          if(widget.continuityScan && result){
            Future.delayed(widget.scanInterval,(){
              Fqreader._startScan();
            });
          }else{
            Fqreader._stopScan();
          }
        });
      }
  }
}

enum ScanType{
  /**
   * 所有条形码
   */
  ALL,
  /**
   *  普通二维码
   */
  QR_CODE,
  /**
   *  二维码 主要用于航空。比如坐飞机行李箱上贴的便签
   */
  AZTEC,
  /**
   * 条形码
   */
  CODABAR,
  /**
   * CODE 39 条形码
   */
  CODE_39,
  /**
   * CODE 92 条形码
   */
  CODE_93,
  /**
   *  CODE 128 条形码
   */
  CODE_128,
  /**
   * 商品用条形码 EAN8
   */
  EAN8,
  /**
   * 商品用条形码 EAN13
   */
  EAN13,
  /**
   * 全球贸易货号。主要用于运输方面的条形码
   */
  ITF,
  /**
   * 一种二维码
   */
  DATA_MATRIX,
  /**
   * PDF417条码是一种高密度、高信息含量的便携式数据文件
   */
  PDF_417
}