package info.geteasy.fqreader;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;


public class ScanView implements Camera.PreviewCallback{
    private FlutterView view;
    private TextureRegistry.SurfaceTextureEntry textureEntry;
    private Camera camera;
    private Rect scanRect;
    private PluginRegistry.Registrar registrar;
    private MultiFormatReader multiFormatReader;
    private EventChannel.EventSink eventSink;

    ScanView(FlutterView view,PluginRegistry.Registrar registrar,Rect scanRect, MethodChannel.Result result){
        this.view = view;
        this.registrar = registrar;
        this.scanRect = scanRect;
        try {
            multiFormatReader = new MultiFormatReader();
            this.textureEntry = view.createSurfaceTexture();
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//            Camera.Parameters param = camera.getParameters();
//            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            camera.setParameters(param);
            camera.setDisplayOrientation(90);
            camera.setPreviewTexture(textureEntry.surfaceTexture());
            result.success(textureEntry.id());
            registerEventChannel();

        }catch (Exception e) {
            result.error("ScanView init",e.getMessage(),null);
        }
    }

    void startScan(){
        camera.setOneShotPreviewCallback(this);
        camera.startPreview();;
    }

    void stopScan(){
        camera.stopPreview();
    }

    void release(){
        camera.release();
        camera = null;
        textureEntry.release();
        textureEntry = null;
    }

    private void registerEventChannel() {
        new EventChannel(
                registrar.messenger(), "fqreader/qrcodeEvents" + textureEntry.id())
                .setStreamHandler(
                        new EventChannel.StreamHandler() {
                            @Override
                            public void onListen(Object arguments, EventChannel.EventSink eventSink) {
                                ScanView.this.eventSink = eventSink;
                            }

                            @Override
                            public void onCancel(Object arguments) {
                                ScanView.this.eventSink = null;
                            }
                        });
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;


            com.google.zxing.Result rawResult = null;
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(bytes, width, height,scanRect.left,scanRect.top,scanRect.width(),scanRect.height(),false);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
            } catch (ReaderException re) {
                // continue
            } catch (NullPointerException npe) {
                // This is terrible
            } catch (ArrayIndexOutOfBoundsException aoe) {

            } finally {
                multiFormatReader.reset();
            }

            if (rawResult == null) {
                LuminanceSource invertedSource = source.invert();
                bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (NotFoundException e) {
                    // continue
                } finally {
                    multiFormatReader.reset();
                }
            }

            if (rawResult != null) {
                eventSink.success(rawResult.getText());
            } else {
                camera.setOneShotPreviewCallback(this);
            }
        } catch(RuntimeException e) {
            // TODO: Terrible hack. It is possible that this method is invoked after camera is released.
            Log.e(TAG, e.toString(), e);
        }
    }
}
