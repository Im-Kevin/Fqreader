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

public class DecodeThread extends  Thread {
    private boolean mExit = false;
    private byte[] mImageBytes;;
    private Rect mScanRect;
    private Camera.Size mCameraSize;
    private Handler mDecodeHandler;
    private MultipleDecode mDecode;

    DecodeThread(
                        Handler decodeHandler,
                        Camera.Size cameraSize,
                        Rect scanRect){
        this.mScanRect = scanRect;
        this.mCameraSize = cameraSize;
        this.mDecodeHandler = decodeHandler;
        mDecode = new MultipleDecode();

        //设置字符集为UTF-8
        Map<DecodeHintType, Object> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        mDecode.setHints(hints);
    }
    void decode(byte[] bytes){
        synchronized (this){
            this.notify();
        }
        this.mImageBytes = bytes;
    }
    void setFormats(List<String> formats){
        mDecode.setFormats(formats);
    }

    void release(){
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
                int width = mCameraSize.width;
                int height = mCameraSize.height;

                com.google.zxing.Result rawResult = null;
                LuminanceSource source = new PlanarYUVLuminanceSource(mImageBytes,
                        width, height,
                        mScanRect.left,mScanRect.top,
                        mScanRect.width(),mScanRect.height(),
                        true);

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
                        LuminanceSource invertedSource = source.rotateCounterClockwise();
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
                    msg.obj = rawResult.getText();
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

}
