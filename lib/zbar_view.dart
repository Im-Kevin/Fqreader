part of fqreader;

class ZBarView extends StatefulWidget {
  final int width;
  final int height;
  final Rect? scanRect;

  final ScanEvent onScan;

  const ZBarView({Key? key,required this.width,required this.height, this.scanRect,required this.onScan})
      : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return ZBarViewState();
  }
}

class ZBarViewState extends State<ZBarView> {
  int? _viewID;
  MethodChannel? _channel;

  @override
  Widget build(BuildContext context) {
    var ratio = MediaQuery.of(context).devicePixelRatio;
    
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: 'info.geteasy.fqreader_view',
        creationParams: {
          'width': 1080,
          'height': 1920,
          'scanLeft': (widget.scanRect?.left.toInt() ?? 0  * ratio).toInt(),
          'scanTop': (widget.scanRect?.top.toInt() ?? 0  * ratio).toInt(),
          'scanBottom': (widget.scanRect?.bottom.toInt() ?? widget.height  * ratio).toInt(),
          'scanRight': (widget.scanRect?.right.toInt() ?? widget.width  * ratio).toInt(),
        },
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: _onPlatformViewCreated,
        gestureRecognizers: <Factory<OneSequenceGestureRecognizer>>[
          new Factory<OneSequenceGestureRecognizer>(
              () => new EagerGestureRecognizer()),
        ].toSet(),
      );
    } else {
      return Container();
    }
  }

  void _onPlatformViewCreated(int id) {
    _viewID = id;
    _channel = MethodChannel('info.geteasy.fqreader_view.event$_viewID');
    _channel!.setMethodCallHandler(_handleMessages);
    this.startCamera();
  }

  Future _handleMessages(MethodCall call) async {
    switch (call.method) {
      case "onScan":
        String data = call.arguments['data'];
        widget.onScan(ScanResult(data: data)).then((value) {
          if (value) {
            this.startCamera();
          }
        });
        break;
    }
  }

  startCamera() async {
    return await _channel!.invokeMethod("startCamera");
  }

  stopCamera() async {
    return await _channel!.invokeMethod("stopCamera");
  }

  setFlash() async {
    return await _channel!.invokeMethod("setFlash");
  }

  Future<bool> getFlash() async {
    bool result = await _channel!.invokeMethod("getFlash");
    return result;
  }
}
