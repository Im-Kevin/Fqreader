package info.geteasy.fqreader.view;

import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import me.dm7.barcodescanner.core.BarcodeScannerView;
import me.dm7.barcodescanner.core.CameraUtils;
import me.dm7.barcodescanner.core.CameraWrapper;

public class ZBarCameraHandlerThread extends HandlerThread {
    private static final String LOG_TAG = "CameraHandlerThread";

    private ZBarView mScannerView;

    public ZBarCameraHandlerThread(ZBarView scannerView) {
        super("CameraHandlerThread");
        mScannerView = scannerView;
        start();
    }

    public void startCamera(final int cameraId) {
        Handler localHandler = new Handler(getLooper());
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                final Camera camera = CameraUtils.getCameraInstance(cameraId);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScannerView.setupCameraPreview(CameraWrapper.getWrapper(camera, cameraId));
                    }
                });
            }
        });
    }
}
