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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

/**
 * FqreaderPlugin
 */
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

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "fqreader");

        channel.setMethodCallHandler(
                new FqreaderPlugin(registrar, registrar.view(), registrar.activity()));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "initView":
                if (scanView != null) {
                    result.error("ScanView", "Scan Already initialized", null);
                    return;
                }
                HashMap<String, Object> viewRectMap = call.argument("viewRect");
                Rect viewRect = new Rect(
                        (int) viewRectMap.get("left"),
                        (int) viewRectMap.get("top"),
                        (int) viewRectMap.get("right"),
                        (int) viewRectMap.get("bottom")
                );
                HashMap<String, Object> scanRectMap = call.argument("scanRect");
                Rect scanRect = new Rect(
                        (int) scanRectMap.get("left"),
                        (int) scanRectMap.get("top"),
                        (int) scanRectMap.get("right"),
                        (int) scanRectMap.get("bottom")
                );
                List<String> scanType = call.argument("scanType");
                scanView = new ScanView(view, registrar,
                        viewRect,
                        scanRect,
                        scanType,
                        result);
                break;
            case "startScan":
                scanView.startScan();
                result.success(null);
                break;
            case "stopScan":
                scanView.stopScan();
                result.success(null);
                break;
            case "turnOn":
                scanView.turnOn();
                result.success(null);
                break;
            case "turnOff":
                scanView.turnOff();
                result.success(null);
                break;
            case "release":
                scanView.release();
                scanView = null;
                break;
            case "decodeImg":
                MultipleDecode decode = new MultipleDecode();
                List<String> decodeImgScanType = call.argument("scanType");
                byte[] imgData = call.argument("image");

                Map<DecodeHintType, Object> hints = new Hashtable<>();
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
                decode.setHints(hints);
                decode.setFormats(decodeImgScanType);
                try {
                    com.google.zxing.Result scanResult  = decode.decode(new BinaryBitmap(
                            new HybridBinarizer(
                                new BitmapLuminanceSource(imgData)
                            )));
                    result.success(scanResult.getText());
                } catch (NotFoundException e) {
                    result.success(null);
                } catch (ChecksumException e) {
                    result.error("ChecksumException ",e.getLocalizedMessage(),e);
//                    e.printStackTrace();
                } catch (FormatException e) {
                    result.error("FormatException ",e.getLocalizedMessage(),e);
//                    e.printStackTrace();
                }catch (Exception e){
                    result.error("Exception ",e.getLocalizedMessage(),e);
                }
                break;
        }
    }

}