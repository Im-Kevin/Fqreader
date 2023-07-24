library fqreader;


import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

part "scan_view.dart";
part 'zbar_view.dart';

typedef ScanEvent = Future<bool> Function(ScanResult value);

class Fqreader {
  static const MethodChannel _channel =
  const MethodChannel('fqreader');

  static Future<ScanResult?> decodeImg(File file,List<ScanType> scanType) async{
    var scanStr = <String>[];
    scanType.forEach((item){
      scanStr.add(item.toString());
    });

    List<int> data = file.readAsBytesSync();
    Uint8List uData = new Uint8List.fromList(data);

    var result = await  _channel.invokeMethod('decodeImg',{
      "image": uData,
      "scanType": scanStr
    });
    if(result == null)
      return null;

    return ScanResult(
      scanType: _parseScanType(result['scanType']),
      data: result['data'],
    );
  }

  static Future<InitResult> _initView({
      required List<ScanType> scanType,
      double devicePixelRatio = 1
  }) async {
    var scanStr = <String>[];
    scanType.forEach((item){
      scanStr.add(item.toString());
    });

    final Map<dynamic,dynamic> result = await _channel.invokeMethod('initView',{
      "scanType": scanStr
    });
    return InitResult(
      cameraSize: Size(result['cameraWidth']  / devicePixelRatio, result['cameraHeight'] / devicePixelRatio),
      textureID: result['textureID']
    );
  }
  static Future _setScanRect(
      Rect scanRect,
      double devicePixelRatio) async{
    await _channel.invokeMethod('setScanRect',{
    "scanRect":{
    "left":(scanRect.left* devicePixelRatio).toInt(),
    "top":(scanRect.top* devicePixelRatio).toInt(),
    "right":(scanRect.right* devicePixelRatio).toInt(),
    "bottom":(scanRect.bottom* devicePixelRatio).toInt(),
    }});
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

class InitResult{
  final Size cameraSize;
  final int textureID;
  const InitResult({required this.cameraSize,required this.textureID});
}

class ScanResult{
  final String data;
  final ScanType? scanType;
  const ScanResult({required this.data,this.scanType});
}

enum ScanType{
  /// 所有条形码
  ALL,
  ///  普通二维码
  QR_CODE,
  ///  二维码 主要用于航空。比如坐飞机行李箱上贴的便签
  AZTEC,
  /// 条形码
  CODABAR,
  /// CODE 39 条形码
  CODE_39,
  /// CODE 92 条形码
  CODE_93,
  ///  CODE 128 条形码
  CODE_128,
  /// 商品用条形码 EAN8
  EAN8,
  /// 商品用条形码 EAN13
  EAN13,
  /// 全球贸易货号。主要用于运输方面的条形码
  ITF,
  /// 一种二维码
  DATA_MATRIX,
  /// PDF417条码是一种高密度、高信息含量的便携式数据文件
  PDF_417,
}

ScanType _parseScanType(String str){
  switch(str){
    case 'ScanType.ALL':
      return ScanType.ALL;
    case 'ScanType.QR_CODE':
      return ScanType.QR_CODE;
    case 'ScanType.AZTEC':
      return ScanType.AZTEC;
    case 'ScanType.CODABAR':
      return ScanType.CODABAR;
    case 'ScanType.CODE_39':
      return ScanType.CODE_39;
    case 'ScanType.CODE_93':
      return ScanType.CODE_93;
    case 'ScanType.CODE_128':
      return ScanType.CODE_128;
    case 'ScanType.EAN8':
      return ScanType.EAN8;
    case 'ScanType.EAN13':
      return ScanType.EAN13;
    case 'ScanType.ITF':
      return ScanType.ITF;
    case 'ScanType.DATA_MATRIX':
      return ScanType.DATA_MATRIX;
    case 'ScanType.PDF_417':
      return ScanType.PDF_417;
  }
  throw new ArgumentError('未知类型');
}
