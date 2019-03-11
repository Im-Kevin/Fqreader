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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.view.FlutterView;
import io.flutter.view.TextureRegistry;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;


public class ScanView {
    private TextureRegistry.SurfaceTextureEntry textureEntry;
    private Camera mCamera;
    private DecodeHandler mDecodeHandler;

    ScanView(FlutterView view,
             PluginRegistry.Registrar registrar,
             Rect viewRect,
             Rect scanRect,
             List<String> scanType,
             MethodChannel.Result result) {
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters param = mCamera.getParameters();
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            param.setRotation(90);

            // 选择最合适的预览图像大小
            Camera.Size currentSize = matchSize(viewRect, param);
            // 设置预览图像大小
            param.setPreviewSize(currentSize.width, currentSize.height);
            mCamera.setParameters(param);
            mCamera.setDisplayOrientation(90); //旋转90度,变成竖屏

            //链接flutter纹理
            this.textureEntry = view.createSurfaceTexture();
            mCamera.setPreviewTexture(textureEntry.surfaceTexture());

            //返回纹理ID
            result.success(textureEntry.id());

            mDecodeHandler = new DecodeHandler(mCamera, scanType, scanRect);
            mDecodeHandler.registerEventChannel(registrar, textureEntry.id());

        } catch (Exception e) {
            result.error("ScanView init", e.getMessage(), null);
        }
    }

    void startScan() {
        mCamera.setOneShotPreviewCallback(mDecodeHandler);
        mCamera.startPreview();
        ;
    }

    void stopScan() {
        mCamera.setOneShotPreviewCallback(null);
        mCamera.stopPreview();
    }

    /**
     * 开灯
     */
    void turnOn() {
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(param);
    }

    /**
     * 关灯
     */
    void turnOff() {
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(param);
    }

    void release() {
        mCamera.release();
        mCamera = null;
        textureEntry.release();
        textureEntry = null;
        mDecodeHandler.release();
    }

    /**
     * 选择最合适的预览图片大小
     *
     * @param viewRect
     * @param param
     * @return
     */
    private Camera.Size matchSize(Rect viewRect, Camera.Parameters param) {
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();

        double viewRatio = viewRect.width() * 1.0 / viewRect.height(); // 获取控件比例
        Camera.Size currentSize = sizes.get(0);
        double currentRatio = currentSize.width * 1.0 / currentSize.height; //获取第一个预览大小的比例

        for (int i = 1; i < sizes.size(); i++) {
            Camera.Size item = sizes.get(i);
            double diffOld = Math.abs(currentRatio - viewRatio); //与控件比例的差异 旧
            double diffNew = Math.abs(item.width * 1.0 / item.height - viewRatio);  //与控件比例的差异 新

            if (diffOld < diffNew) { //如果旧的小于新的则使用新的预览大小
                currentSize = item;
                currentRatio = item.width * 1.0 / item.height;
            } else if (diffOld == diffNew) {
                if (item.height > currentSize.height) { //如果一样且新的像素大于旧的,则用新的
                    currentSize = item;
                }
            }
        }
        return currentSize;
    }


}
