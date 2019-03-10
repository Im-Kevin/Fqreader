library fqreader;

import 'dart:async';

import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


typedef ScanEvent = Future<bool> Function(String value);

class _Fqreader {
  static const MethodChannel _channel =
  const MethodChannel('fqreader');

  static Future<int> initView({
      @required Rect viewRect,
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
      },
      "scanType": scanStr
    });
    return textureId;
  }
  static Future startScan() async{
    await _channel.invokeMethod('startScan');
  }

  static Future stopScan() async{
    await _channel.invokeMethod('stopScan');
  }
  static Future turnOn() async{
    await _channel.invokeMethod("turnOn");
  }
  static Future turnOff() async{
    await _channel.invokeMethod("turnOff");
  }
  static Future release() async{
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
  final Rect viewRect;

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
      @required this.viewRect,
      @required this.scanRect,
      this.scanType = const [ScanType.QR_CODE],
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
    _Fqreader.initView(
        viewRect: widget.viewRect,
        scanRect:widget.scanRect,
        devicePixelRatio:mediaQuery.devicePixelRatio,
        scanType: widget.scanType
    ).then((textureId){
      setState(() {
        _textureId = textureId;
      });
      if(widget.autoScan){
        _Fqreader.startScan();
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
    _Fqreader.release();
  }

  /**
   * 开始扫描
   */
  Future startScan() async{
    await _Fqreader.startScan();
  }

  /**
   * 暂停扫描
   */
  Future stopScan() async{
    await _Fqreader.stopScan();
  }
  /**
   * 开灯
   */
  Future turnOn() async{
    await _Fqreader.turnOn();
  }
  /**
   * 关灯
   */
  Future turnOff() async{
    await _Fqreader.turnOff();
  }

  void _listener(dynamic value) {
    if(widget != null)
      {
        if(!widget.continuityScan) //是否连续扫描
          {
            _Fqreader.stopScan();
          }
        widget.onScan(value).then((result){
          if(widget.continuityScan && result){
            Future.delayed(widget.scanInterval,(){
              _Fqreader.startScan();
            });
          }else{
            _Fqreader.stopScan();
          }
        });
      }
  }
}

enum ScanType{
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