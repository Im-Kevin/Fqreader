part of fqreader;

class ScanView extends StatefulWidget {
  /// 扫描事件
  final ScanEvent onScan;

  /// 扫描区域位置大小
  final Rect? scanRect;

  /// 扫描区域大小
  final Size? scanSize;

  /// ScanView控件大小
  final Size viewSize;

  /// 扫描框的位置(位于图片)
  final Alignment scanAilgn;

  /// view的位置(位于图片)
  final Alignment viewAilgn;

  /// 屏幕分辨率
  final double devicePixelRatio;

  final Color? maskColor;

  /// 是否立即扫描
  final bool autoScan;

  /// 是否连续扫描
  final bool continuityScan;

  /// 连续扫描间隔
  final Duration scanInterval;

  ///  扫描的条码类型
  final List<ScanType> scanType;

  const ScanView(
      {Key? key,
      required this.onScan,
      required this.viewSize,
      this.scanRect,
      this.scanSize,
      this.scanAilgn = Alignment.topLeft,
      this.viewAilgn = Alignment.topLeft,
      required this.devicePixelRatio,
      this.maskColor,
      this.scanType = const [ScanType.ALL],
      this.autoScan = true,
      this.continuityScan = false,
      this.scanInterval = const Duration(milliseconds: 500)})
      : assert(scanSize != null || scanRect != null),
        assert(!(scanSize != null && scanRect != null)),
        super(key: key);

  @override
  State<StatefulWidget> createState() => ScanViewState();
}

class ScanViewState extends State<ScanView> {
  int? _textureId;
  StreamSubscription? _readySubscription;
  Size? _cameraSize;
  double? _cameraPixelRatio;

  @override
  void initState() {
    super.initState();
    if (widget.autoScan) {
      this.startScan();
    }
  }

  @override
  Widget build(BuildContext context) {
    return _textureId != null
        ? new _ScanViewTexture(
            textureId: _textureId!,
            cameraSize: _cameraSize!,
            scanRect: widget.scanRect ??
                _getViewRect(_cameraSize!, widget.scanSize!, widget.scanAilgn),
            maskColor: widget.maskColor,
            viewRect:
                _getViewRect(_cameraSize!, widget.viewSize, widget.viewAilgn),
          )
        : new SizedBox(
            width: widget.viewSize.width, height: widget.viewSize.height);
  }

  Rect _getViewRect(Size cameraSize, Size viewSize, Alignment alignment) {
    var offset = (cameraSize - viewSize) as Offset;
    return alignment.alongOffset(offset) & viewSize;
  }

  Size _cameraToViewSize(Size cameraSize) {
    return Size(cameraSize.width * _cameraPixelRatio!,
        cameraSize.height * _cameraPixelRatio!);
  }

  Rect _viewToCameraRect(Rect viewSize) {
    return Rect.fromLTWH(
        viewSize.left / _cameraPixelRatio!,
        viewSize.top / _cameraPixelRatio!,
        viewSize.width / _cameraPixelRatio!,
        viewSize.height / _cameraPixelRatio!);
  }

  @override
  void dispose() {
    super.dispose();
    this.release();
  }

  /// 开始扫描
  Future startScan() async {
    if (this._textureId == null) {
      var initResult = await Fqreader._initView(
          devicePixelRatio: widget.devicePixelRatio, scanType: widget.scanType);
      _cameraPixelRatio = widget.viewSize.width / initResult.cameraSize.width;
      _cameraSize = _cameraToViewSize(initResult.cameraSize);
      _textureId = initResult.textureID;
      Rect scanRect = widget.scanRect ??
          _getViewRect(_cameraSize!, widget.scanSize!, widget.scanAilgn);

      var left = scanRect.left < 0.0 ? 0.0 : scanRect.left;
      var top = scanRect.top < 0.0 ? 0.0 : scanRect.top;
      var width = (left + scanRect.width > _cameraSize!.width)
          ? _cameraSize!.width - left
          : scanRect.width;
      var height = (top + scanRect.height > _cameraSize!.height)
          ? _cameraSize!.height - top
          : scanRect.height;
      scanRect = Rect.fromLTWH(left, top, width, height); // 确保不超出界限

      scanRect = _viewToCameraRect(scanRect);
      Fqreader._setScanRect(scanRect, widget.devicePixelRatio);

      _readySubscription = new EventChannel('fqreader/scanEvents$_textureId')
          .receiveBroadcastStream()
          .listen(_listener);
      setState(() {});
    } else {
      if (_readySubscription == null) {
        _readySubscription = new EventChannel('fqreader/scanEvents$_textureId')
            .receiveBroadcastStream()
            .listen(_listener);
        setState(() {});
      }
    }
    await Fqreader._startScan();
  }

  /// 暂停扫描
  Future stopScan() async {
    await Fqreader._stopScan();
    await _readySubscription?.cancel();
    _readySubscription = null;
  }

  /// 开灯
  Future turnOn() async {
    await Fqreader._turnOn();
  }

  /// 关灯
  Future turnOff() async {
    await Fqreader._turnOff();
  }

  Future release() async {
    if (this._textureId != null) {
      this._textureId = null;
      try {
        setState(() {});
      } catch (_) {}
      await _readySubscription?.cancel();
    _readySubscription = null;
      await Fqreader._release();
    }
  }

  void _listener(dynamic value) {
    if (!widget.continuityScan) //是否连续扫描
    {
      Fqreader._stopScan();
    }

    var result = ScanResult(
      scanType: _parseScanType(value['scanType']),
      data: value['data'],
    );
    widget.onScan(result).then((result) {
      if (widget.continuityScan && result) {
        Future.delayed(widget.scanInterval, () {
          Fqreader._startScan();
        });
      } else {
        Fqreader._stopScan();
      }
    });
  }
}

class _ScanViewTexture extends LeafRenderObjectWidget {
  final Size cameraSize;
  final Rect viewRect;
  final Rect? scanRect;
  final Color? maskColor;

  /// Creates a widget backed by the texture identified by [textureId].
  const _ScanViewTexture(
      {Key? key,
      required this.cameraSize,
      required this.viewRect,
      required this.textureId,
      this.scanRect,
      this.maskColor})
      : super(key: key);

  /// The identity of the backend texture.
  final int textureId;

  @override
  _ScanViewTextureBox createRenderObject(BuildContext context) =>
      _ScanViewTextureBox(
          textureId: textureId,
          cameraSize: cameraSize,
          viewRect: viewRect,
          maskColor: maskColor,
          scanRect: scanRect);

  @override
  void updateRenderObject(
      BuildContext context, _ScanViewTextureBox renderObject) {
    renderObject.textureId = textureId;
    renderObject.maskColor = maskColor;
    renderObject.cameraSize = cameraSize;
    renderObject.viewRect = viewRect;
    renderObject.scanRect = scanRect;
  }
}

class _ScanViewTextureBox extends RenderBox {
  /// Creates a box backed by the texture identified by [textureId].
  _ScanViewTextureBox(
      {required int textureId,
      required Size cameraSize,
      required Rect viewRect,
      required Rect? scanRect,
      Color? maskColor})
      : _textureId = textureId,
        _cameraSize = cameraSize,
        _viewRect = viewRect,
        _scanRect = scanRect,
        _maskColor = maskColor;

  /// The identity of the backend texture.
  int? get textureId => _textureId;
  int? _textureId;
  set textureId(int? value) {
    if (value != _textureId) {
      _textureId = value;
      markNeedsPaint();
    }
  }

  Size get cameraSize => _cameraSize;
  Size _cameraSize;
  set cameraSize(Size value) {
    if (value != _cameraSize) {
      _cameraSize = value;
      markNeedsPaint();
    }
  }

  Rect get viewRect => _viewRect;
  Rect _viewRect;
  set viewRect(Rect value) {
    if (value != _viewRect) {
      _viewRect = value;
      markNeedsPaint();
    }
  }

  Rect? get scanRect => _scanRect;
  Rect? _scanRect;
  set scanRect(Rect? value) {
    if (value != _scanRect) {
      _scanRect = value;
      markNeedsPaint();
    }
  }

  Color? get maskColor => _maskColor;
  Color? _maskColor;
  set maskColor(Color? value) {
    if (value != _maskColor) {
      _maskColor = value;
      markNeedsPaint();
    }
  }

  @override
  bool get sizedByParent => true;

  @override
  bool get alwaysNeedsCompositing => true;

  @override
  bool get isRepaintBoundary => true;

  @override
  void performResize() {
    size = viewRect.size;
  }

  @override
  bool hitTestSelf(Offset position) => true;

  @override
  void paint(PaintingContext context, Offset offset) {
    if (_textureId == null) return;
    var cameraSize = Size(viewRect.width,
        viewRect.width * (_cameraSize.height / _cameraSize.width));
    context.pushClipRect(needsCompositing, offset,
        Rect.fromLTWH(0, 0, viewRect.width, viewRect.height),
        (PaintingContext context, Offset offset) {
      context.addLayer(TextureLayer(
        rect: Rect.fromLTWH(offset.dx - viewRect.left, offset.dy - viewRect.top,
            cameraSize.width, cameraSize.height),
        textureId: _textureId!,
      ));
      if (maskColor != null) {
        Paint paint = Paint()..color = maskColor!;
        context.canvas.drawRect(
            Rect.fromLTWH(0, 0, viewRect.width, scanRect!.top), paint);
        context.canvas.drawRect(
            Rect.fromLTWH(0, scanRect!.top, scanRect!.left, scanRect!.height),
            paint);
        context.canvas.drawRect(
            Rect.fromLTWH(0, scanRect!.bottom, viewRect.width,
                viewRect.height - scanRect!.bottom),
            paint);
        context.canvas.drawRect(
            Rect.fromLTWH(scanRect!.right, scanRect!.top,
                viewRect.width - scanRect!.right, scanRect!.height),
            paint);
      }
    });
  }
}
