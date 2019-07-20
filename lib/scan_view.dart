part of fqreader;


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

      var result = ScanResult(
        scanType: _parseScanType(value['scanType']),
        data: value['data'],
      );
      widget.onScan(result).then((result){
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
