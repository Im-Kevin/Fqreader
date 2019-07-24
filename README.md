# fqreader [![pub package](https://img.shields.io/pub/v/fqreader.svg)](https://pub.dartlang.org/packages/fqreader)

一个可以自定义的扫描控件,

Usage
Add this to your package's pubspec.yaml file:
``` yaml
dependencies:
  fqreader: "^0.0.2"
```

## Fqreader

#### decodeImg
解析图片

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| [file] | <code>File<Widget></code>  | | 图片文件 |
| [[scanType](#ScanType)] | <code>List<ScanType><Widget></code> | ScanType.ALL| 扫描的类型,默认全部类型 |

返回

## ScanView

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| [onScan] | <code>ScanEvent</code> | | 扫描事件 |
| [scanSize] | <code>Size</code> | | 扫描区域大小 |
| [pictureSize] | <code>Size</code> | | 预览图片大小 |
| [viewSize] | <code>Size</code> | | ScanView控件大小 |
| [scanAilgn] | <code>Alignment</code> | | 扫描框的位置(位于图片) |
| [viewAilgn] | <code>Alignment</code> | | view的位置(位于图片) |
| [autoScan] | <code>bool</code> | true | 是否立即扫描 |
| [continuityScan] | <code>bool</code> | false | 是否连续扫描 |
| [scanInterval] | <code>Duration</code> |  0.5s  | 连续扫描间隔 |
| [scanType] | <code>List<ScanType></code> | ScanType.ALL |  扫描的条码类型 |
| [devicePixelRatio] | <code>double</code> | | 屏幕分辨率 |

## ScanViewState

#### startScan
开始扫描 


#### stopScan
暂停扫描 

#### turnOn
开灯

#### turnOff
关灯

## ScanType
| Enum |  Description |
| --- |  --- |
| [ALL] |  所有条形码 |
| [QR_CODE] |  普通二维码 |
| [AZTEC] |  二维码 主要用于航空。比如坐飞机行李箱上贴的便签 |
| [CODABAR] |  条形码|
| [CODE_39] |  CODE 39 条形码|
| [CODE_93] |  CODE 92 条形码|
| [CODE_128] |   CODE 128 条形码|
| [EAN8] |    商品用条形码 EAN8|
| [EAN13] |   商品用条形码 EAN13|
| [ITF] |   全球贸易货号。主要用于运输方面的条形码|
| [DATA_MATRIX] |  一种二维码 |
| [PDF_417] |  PDF417条码是一种高密度、高信息含量的便携式数据文件 |


