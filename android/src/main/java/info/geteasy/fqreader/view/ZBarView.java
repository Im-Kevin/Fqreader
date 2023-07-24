package info.geteasy.fqreader.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import me.dm7.barcodescanner.core.CameraHandlerThread;
import me.dm7.barcodescanner.core.CameraPreview;
import me.dm7.barcodescanner.core.CameraUtils;
import me.dm7.barcodescanner.core.CameraWrapper;
import me.dm7.barcodescanner.core.DisplayUtils;
import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.core.ViewFinderView;
import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class ZBarView  extends FrameLayout implements Camera.PreviewCallback   {
    private static final String TAG = "ZBarView";

    private CameraWrapper mCameraWrapper;
    private CameraPreview mPreview;
    private Rect mFramingRectInPreview;
    private Rect mScanRect;
    private ZBarCameraHandlerThread mCameraHandlerThread;
    private Boolean mFlashState;
    private boolean mAutofocusState = true;
    private boolean mShouldScaleToFill = true;


    public interface ResultHandler {
        public void handleResult(Result rawResult);
    }


    static {
        System.loadLibrary("iconv");
    }

    private ImageScanner mScanner;
    private List<BarcodeFormat> mFormats;
    private ZBarScannerView.ResultHandler mResultHandler;



    public ZBarView(Context context) {
        super(context);
        setFormats(BarcodeFormat.ALL_FORMATS);
    }

    public void setFormats(List<BarcodeFormat> formats) {
        mFormats = formats;
        setupScanner();
    }

    public void setResultHandler(ZBarScannerView.ResultHandler resultHandler) {
        mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        if(mFormats == null) {
            return BarcodeFormat.ALL_FORMATS;
        }
        return mFormats;
    }

    public void setupScanner() {
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);

        mScanner.setConfig(Symbol.NONE, Config.ENABLE, 0);
        for(BarcodeFormat format : getFormats()) {
            mScanner.setConfig(format.getId(), Config.ENABLE, 1);
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(mResultHandler == null) {
            return;
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;

            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
                int rotationCount = getRotationCount();
                if (rotationCount == 1 || rotationCount == 3) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
                data = getRotatedData(data, camera);
            }

            Rect rect = getFramingRectInPreview(width, height);
            Image barcode = new Image(width, height, "Y800");
            barcode.setData(data);
            barcode.setCrop(rect.left, rect.top, rect.width(), rect.height());

            int result = mScanner.scanImage(barcode);

            if (result != 0) {
                SymbolSet syms = mScanner.getResults();
                final Result rawResult = new Result();
                for (Symbol sym : syms) {
                    // In order to retreive QR codes containing null bytes we need to
                    // use getDataBytes() rather than getData() which uses C strings.
                    // Weirdly ZBar transforms all data to UTF-8, even the data returned
                    // by getDataBytes() so we have to decode it as UTF-8.
                    String symData;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        symData = new String(sym.getDataBytes(), StandardCharsets.UTF_8);
                    } else {
                        symData = sym.getData();
                    }
                    if (!TextUtils.isEmpty(symData)) {
                        rawResult.setContents(symData);
                        rawResult.setBarcodeFormat(BarcodeFormat.getFormatById(sym.getType()));
                        break;
                    }
                }

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Stopping the preview can take a little long.
                        // So we want to set result handler to null to discard subsequent calls to
                        // onPreviewFrame.
                        ZBarScannerView.ResultHandler tmpResultHandler = mResultHandler;
                        mResultHandler = null;

                        stopCameraPreview();
                        if (tmpResultHandler != null) {
                            tmpResultHandler.handleResult(rawResult);
                        }
                    }
                });
            } else {
                camera.setOneShotPreviewCallback(this);
            }
        } catch(RuntimeException e) {
            // TODO: Terrible hack. It is possible that this method is invoked after camera is released.
            Log.e(TAG, e.toString(), e);
        }
    }

    public void resumeCameraPreview(ZBarScannerView.ResultHandler resultHandler) {
        mResultHandler = resultHandler;
        resumeCameraPreview();
    }


    public final void setupLayout(CameraWrapper cameraWrapper) {
        removeAllViews();

        mPreview = new CameraPreview(getContext(), cameraWrapper, this);
        mPreview.setAspectTolerance(0.1f);
        mPreview.setShouldScaleToFill(mShouldScaleToFill);
        if (!mShouldScaleToFill) {
            RelativeLayout relativeLayout = new RelativeLayout(getContext());
            relativeLayout.setGravity(Gravity.CENTER);
            relativeLayout.setBackgroundColor(Color.BLACK);
            relativeLayout.addView(mPreview);
            addView(relativeLayout);
        } else {
            addView(mPreview);
        }
    }


    public void startCamera(int cameraId) {
        if(mCameraHandlerThread == null) {
            mCameraHandlerThread = new ZBarCameraHandlerThread(this);
        }
        mCameraHandlerThread.startCamera(cameraId);
    }

    public void setupCameraPreview(CameraWrapper cameraWrapper) {
        mCameraWrapper = cameraWrapper;
        if(mCameraWrapper != null) {
            setupLayout(mCameraWrapper);
            if(mFlashState != null) {
                setFlash(mFlashState);
            }
            setAutoFocus(mAutofocusState);
        }
    }

    public void startCamera() {
        startCamera(CameraUtils.getDefaultCameraId());
    }

    public void stopCamera() {
        if(mCameraWrapper != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            mCameraWrapper.mCamera.release();
            mCameraWrapper = null;
        }
        if(mCameraHandlerThread != null) {
            mCameraHandlerThread.quit();
            mCameraHandlerThread = null;
        }
    }

    public void stopCameraPreview() {
        if(mPreview != null) {
            mPreview.stopCameraPreview();
        }
    }

    protected void resumeCameraPreview() {
        if(mPreview != null) {
            mPreview.showCameraPreview();
        }
    }

    public Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        synchronized (this){
            if (mFramingRectInPreview == null) {
                if(mScanRect != null){

                    Rect rect = new Rect(mScanRect);
                    int viewFinderViewWidth = mScanRect.width();
                    int viewFinderViewHeight = mScanRect.height();

                    if(previewWidth < viewFinderViewWidth) {
                        rect.left = rect.left * previewWidth / viewFinderViewWidth;
                        rect.right = rect.right * previewWidth / viewFinderViewWidth;
                    }

                    if(previewHeight < viewFinderViewHeight) {
                        rect.top = rect.top * previewHeight / viewFinderViewHeight;
                        rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;
                    }
                    mFramingRectInPreview = rect;
                }else{
                    mFramingRectInPreview = new Rect(0,0,previewWidth,previewHeight);
                }
            }
            return mFramingRectInPreview;
        }
    }

    public void setFramingRectInPreview(Rect rect){
        synchronized (this){
            mScanRect = rect;
            mFramingRectInPreview = null;
        }
    }

    public void setFlash(boolean flag) {
        mFlashState = flag;
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {

            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(flag) {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public boolean getFlash() {
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void toggleFlash() {
        if(mCameraWrapper != null && CameraUtils.isFlashSupported(mCameraWrapper.mCamera)) {
            Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCameraWrapper.mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        mAutofocusState = state;
        if(mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }

    public void setShouldScaleToFill(boolean shouldScaleToFill) {
        mShouldScaleToFill = shouldScaleToFill;
    }

    public byte[] getRotatedData(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;

        int rotationCount = getRotationCount();

        if(rotationCount == 1 || rotationCount == 3) {
            for (int i = 0; i < rotationCount; i++) {
                byte[] rotatedData = new byte[data.length];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++)
                        rotatedData[x * height + height - y - 1] = data[x + y * width];
                }
                data = rotatedData;
                int tmp = width;
                width = height;
                height = tmp;
            }
        }

        return data;
    }

    public int getRotationCount() {
        int displayOrientation = mPreview.getDisplayOrientation();
        return displayOrientation / 90;
    }
}
