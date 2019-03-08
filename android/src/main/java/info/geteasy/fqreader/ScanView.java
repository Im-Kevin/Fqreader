package info.geteasy.fqreader;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;


public class ScanView implements Camera.PreviewCallback,Runnable{
    private FlutterView view;
    private TextureRegistry.SurfaceTextureEntry textureEntry;
    private Camera camera;
    private Rect scanRect;
    private PluginRegistry.Registrar registrar;
    private MultiFormatReader multiFormatReader;
    private EventChannel.EventSink eventSink;
    private Handler resultHandler;
    private Thread decodeThread;
    private byte[] imageBytes;

    ScanView(FlutterView view,PluginRegistry.Registrar registrar,Rect viewRect,Rect scanRect, MethodChannel.Result result){
        this.view = view;
        this.registrar = registrar;
        this.scanRect = scanRect;
        try {
            multiFormatReader = new MultiFormatReader();
            this.textureEntry = view.createSurfaceTexture();
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters param = camera.getParameters();
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            param.setRotation(90);
            double viewRatio = viewRect.width() * 1.0 / viewRect.height();
            Camera.Size currentSize = sizes.get(0);
            double currentRatio = currentSize.width * 1.0 / currentSize.height;
            for(int i =1;i<sizes.size();i++){
                Camera.Size  item= sizes.get(i);
                double diffOld = Math.abs(currentRatio - viewRatio);
                double diffNew = Math.abs(item.width * 1.0 / item.height - viewRatio);
                if(diffOld < diffNew){
                    currentSize = item;
                    currentRatio = item.width * 1.0 / item.height;
                }else if(diffOld == diffNew){
                    if(item.height > currentSize.height){
                        currentSize = item;
                    }
                }
            }

            param.setPreviewSize(currentSize.width,currentSize.height); // 设置预览图像大小
            camera.setParameters(param);
            camera.setDisplayOrientation(90);
            camera.setPreviewTexture(textureEntry.surfaceTexture());
            result.success(textureEntry.id());
            registerEventChannel();
            decodeThread = new Thread(this);
            decodeThread.start();
            resultHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what){
                        case 1://继续扫描
                            camera.setOneShotPreviewCallback(ScanView.this);
                            break;
                        case 2: //停止扫描
                            eventSink.success((String) msg.obj);
                            break;
                    }
                }
            };

        }catch (Exception e) {
            result.error("ScanView init",e.getMessage(),null);
        }
    }

    void startScan(){
        camera.setOneShotPreviewCallback(this);
        camera.startPreview();;
    }

    void stopScan(){
        camera.setOneShotPreviewCallback(null);
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
        synchronized (this){
            this.imageBytes = bytes;
            this.notify();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    this.wait();
                }
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                int width = size.width;
                int height = size.height;


                com.google.zxing.Result rawResult = null;
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(imageBytes, width, height, scanRect.top, scanRect.left, scanRect.height(), scanRect.width(), false);

                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decode(bitmap);
                } catch (ReaderException re) {
                    // continue
                } catch (NullPointerException npe) {
                    // This is terrible
                } catch (ArrayIndexOutOfBoundsException aoe) {

                } finally {
                    multiFormatReader.reset();
                }
//
//                    if (rawResult == null) {
//                        LuminanceSource invertedSource = source.invert();
//                        bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
//                        try {
//                            rawResult = multiFormatReader.decodeWithState(bitmap);
//
//                        } catch (NotFoundException e) {
//                            // continue
//                        } finally {
//                            multiFormatReader.reset();
//                        }
//                    }

                if (rawResult != null) {
                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = rawResult.getText();
                    this.resultHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = 1;
                    this.resultHandler.sendMessage(msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
