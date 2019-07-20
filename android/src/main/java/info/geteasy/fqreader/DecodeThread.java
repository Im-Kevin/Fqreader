package info.geteasy.fqreader;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.maxicode.MaxiCodeReader;
import com.google.zxing.oned.CodaBarReader;
import com.google.zxing.oned.Code128Reader;
import com.google.zxing.oned.Code39Reader;
import com.google.zxing.oned.Code93Reader;
import com.google.zxing.oned.EAN13Reader;
import com.google.zxing.oned.EAN8Reader;
import com.google.zxing.oned.ITFReader;
import com.google.zxing.oned.MultiFormatOneDReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class DecodeThread extends Thread {
    private boolean mExit = false;
    private byte[] mImageBytes;
    ;
    private Rect mScanRect;
    private Camera mCamera;
    private Camera.Size mCameraSize;
    private Handler mDecodeHandler;
    private MultipleDecode mDecode;

    DecodeThread(
            Handler decodeHandler,
            Camera camera,
            Rect scanRect) {
        this.mScanRect = scanRect;
        this.mCamera = camera;
        this.mCameraSize = camera.getParameters().getPreviewSize();
        this.mDecodeHandler = decodeHandler;
        mDecode = new MultipleDecode();

        //设置字符集为UTF-8
        Map<DecodeHintType, Object> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        mDecode.setHints(hints);
    }

    void decode(byte[] bytes) {
        synchronized (this) {
            this.notify();
        }
        this.mImageBytes = bytes;
    }

    void setFormats(List<String> formats) {
        mDecode.setFormats(formats);
    }

    void release() {
        synchronized (this) {
            mExit = true;
            mImageBytes = null;
            this.notify();//等待下一次解码
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                this.wait();//等待开始
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (!mExit) {
            try {
                byte[] data = mImageBytes;
                int width = mCameraSize.width;
                int height = mCameraSize.height;
                int rotationCount = getRotationCount();
                if (rotationCount == 1 || rotationCount == 3) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
                data = getRotatedData(data, mCamera);

                com.google.zxing.Result rawResult = null;
                LuminanceSource source = new PlanarYUVLuminanceSource(data,
                        width, height,
                        mScanRect.left, mScanRect.top,
                        mScanRect.width(), mScanRect.height(),
                        false);

                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {

                    rawResult = mDecode.decode(bitmap);
                } catch (ReaderException re) {
                    // continue
                } catch (NullPointerException npe) {
                    // This is terrible
                } catch (ArrayIndexOutOfBoundsException aoe) {
                    //
                } finally {
                    mDecode.reset();
                }

                if (rawResult == null) {
                    LuminanceSource invertedSource = source.invert();
                    bitmap = new BinaryBitmap(new HybridBinarizer(invertedSource));
                    try {
                        rawResult = mDecode.decode(bitmap);

                    } catch (NotFoundException e) {
                        // continue
                    } finally {
                        mDecode.reset();
                    }
                }

                if (rawResult != null) {
                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = mDecode.toFlutterMap(rawResult);
                    this.mDecodeHandler.sendMessage(msg);
                } else {
                    Message msg = new Message();
                    msg.what = 1;
                    this.mDecodeHandler.sendMessage(msg);
                }

                synchronized (this) {
                    this.wait();//等待下一次解码
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public byte[] getRotatedData(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int width = size.width;
        int height = size.height;

        int rotationCount = getRotationCount();

        if (rotationCount == 1 || rotationCount == 3) {
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
        return 1;
    }
}
