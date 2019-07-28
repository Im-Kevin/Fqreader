package info.geteasy.fqreader;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

import java.util.List;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry;

public class DecodeHandler extends Handler implements  Camera.PreviewCallback {
    private Camera mCamera;
    private DecodeThread mThread;
    private EventChannel.EventSink eventSink;
    private boolean mRelease;
    DecodeHandler(Camera camera,
                  List<String> scanType){
        mCamera = camera;
        mThread = new DecodeThread(this,mCamera);
        mThread.setFormats(scanType);
        mThread.start();
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if(mRelease){
            return;
        }
        switch (msg.what){
            case 1://继续扫描
                mCamera.setOneShotPreviewCallback(DecodeHandler.this);
                break;
            case 2: //停止扫描
                if(eventSink != null)
                    eventSink.success(msg.obj);
                break;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        mThread.decode(data);
    }

    /**
     * 释放资源
     */
    void release(){
        mRelease = true;
        mThread.release();
    }
    /**
     * 注册通知事件
     */
    void registerEventChannel(PluginRegistry.Registrar registrar, long textureEntryId) {
        new EventChannel(
                registrar.messenger(), "fqreader/scanEvents" + textureEntryId)
                .setStreamHandler(
                        new EventChannel.StreamHandler() {
                            @Override
                            public void onListen(Object arguments, EventChannel.EventSink eventSink) {
                                DecodeHandler.this.eventSink = eventSink;
                            }

                            @Override
                            public void onCancel(Object arguments) {
                                DecodeHandler.this.eventSink = null;
                            }
                        });
    }

    void setScanRect(Rect scanRect){
        mThread.setScanRect(scanRect);
    }
}
