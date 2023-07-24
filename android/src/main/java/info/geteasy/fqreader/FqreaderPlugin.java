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
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import info.geteasy.fqreader.factory.ZBarFactory;
import info.geteasy.fqreader.view.FqreaderPlatformView;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
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
public class FqreaderPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private static final String CHANNEL_VIEW_NAME = "info.geteasy.fqreader_view";

    private static FlutterPluginBinding flutterPluginBinding;
    private static MethodChannel channel;

    private TextureRegistry textureRegistry;
    private Activity activity;
    private BinaryMessenger messenger;

    private ScanView scanView;

    public FqreaderPlugin(){}

    private FqreaderPlugin(
            BinaryMessenger messenger,
            TextureRegistry textureRegistry, Activity activity) {
        this.messenger = messenger;
        this.textureRegistry = textureRegistry;
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
        channel = new MethodChannel(registrar.messenger(), "fqreader");

        channel.setMethodCallHandler(
                new FqreaderPlugin(registrar.messenger(), registrar.view(), registrar.activity()));
        registrar.platformViewRegistry().registerViewFactory(CHANNEL_VIEW_NAME, new ZBarFactory(registrar));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "initView":
                if (scanView != null) {
                    result.error("ScanView", "Scan Already initialized", null);
                    return;
                }
                List<String> scanType = call.argument("scanType");
                scanView = new ScanView(textureRegistry, messenger, activity,
                        scanType,
                        result);
                break;
            case "setScanRect":
                HashMap<String, Object> scanRectMap = call.argument("scanRect");
                Rect scanRect = new Rect(
                        (int) scanRectMap.get("left"),
                        (int) scanRectMap.get("top"),
                        (int) scanRectMap.get("right"),
                        (int) scanRectMap.get("bottom")
                );
                scanView.setScanRect(scanRect);
                result.success(null);
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
                if(scanView != null){
                    synchronized(scanView){
                        if(scanView != null) {
                            scanView.release();
                            scanView = null;
                        }
                    }
                }
                break;
            case "decodeImg":
                decodeImg(call, result);
                break;
        }
    }

    private void decodeImg(MethodCall call, Result result) {
        MultipleDecode decode = new MultipleDecode();
        List<String> scanType = call.argument("scanType");
        byte[] imgData = call.argument("image");

        Map<DecodeHintType, Object> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        decode.setHints(hints);
        decode.setFormats(scanType);
        BitmapLuminanceSource source = new BitmapLuminanceSource(imgData);
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(
                        source
                ));
        com.google.zxing.Result scanResult = null;
        try {
            scanResult = decode.decode(bitmap);
        } catch (NotFoundException ignored) {

        } catch (Exception e){
        }
        finally {
            decode.reset();
        }
        if (scanResult == null) {
            LuminanceSource invertedSource = source.invert();
            bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
            try {
                scanResult = decode.decode(bitmap);
            } catch (NotFoundException ignored) {
            } catch (Exception e){
            }
            finally {
                decode.reset();
            }
        }
        if(scanResult != null){
            result.success(decode.toFlutterMap((scanResult)));
        }else{
            result.success(null);
        }
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding;
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding flutterPluginBinding) {
        this.flutterPluginBinding = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "fqreader");
        channel.setMethodCallHandler(new FqreaderPlugin(flutterPluginBinding.getFlutterEngine().getDartExecutor(), flutterPluginBinding.getFlutterEngine().getRenderer(), activityPluginBinding.getActivity()));
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
        onAttachedToActivity(activityPluginBinding);
    }

    @Override
    public void onDetachedFromActivity() {
    }
}
