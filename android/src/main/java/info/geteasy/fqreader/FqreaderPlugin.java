package info.geteasy.fqreader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import java.util.HashMap;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

/** FqreaderPlugin */
public class FqreaderPlugin implements MethodCallHandler {


  private FlutterView view;
  private Activity activity;
  private Registrar registrar;

  private ScanView scanView;

  private FqreaderPlugin(Registrar registrar, FlutterView view, Activity activity) {
    this.registrar = registrar;
    this.view = view;
    this.activity = activity;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
      }
    }
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "fqreader");

    channel.setMethodCallHandler(
            new FqreaderPlugin(registrar, registrar.view(), registrar.activity()));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method){
      case "initView":
        if(scanView != null){
          result.error("ScanView","Scan Already initialized",null);
          return;
        }
        HashMap<String,Object> viewRectMap = call.argument("viewRect");
        Rect viewRect = new Rect(
                (int)viewRectMap .get("left"),
                (int)viewRectMap .get("top"),
                (int)viewRectMap .get("right"),
                (int)viewRectMap .get("bottom")
        );
        HashMap<String,Object> scanRectMap = call.argument("scanRect");
        Rect scanRect = new Rect(
                (int)scanRectMap.get("left"),
                (int)scanRectMap.get("top"),
                (int)scanRectMap.get("right"),
                (int)scanRectMap.get("bottom")
        );
        scanView = new ScanView(view,registrar,viewRect,scanRect,result);
        break;
      case "startScan":
        scanView.startScan();
        result.success(null);
        break;
      case "stopScan":
        scanView.stopScan();
        result.success(null);
        break;
      case "release":
        scanView.release();
        scanView = null;
        break;
    }
  }

}
