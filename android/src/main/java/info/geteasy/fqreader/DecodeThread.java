package info.geteasy.fqreader;

import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
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
    public boolean exit = false;
    private Map<DecodeHintType, Object> mHints = new Hashtable<>();
    private byte[] mImageBytes;;
    private Rect mScanRect;
    private Camera.Size mCameraSize;
    private Handler mDecodeHandler;
    private Reader[] mReaders;

    public DecodeThread(
                        Handler decodeHandler,
                        Camera.Size cameraSize,
                        Rect scanRect){
        this.mScanRect = scanRect;
        this.mCameraSize = cameraSize;
        this.mDecodeHandler = decodeHandler;

        //设置字符集为UTF-8
        mHints.put(DecodeHintType.CHARACTER_SET, "utf-8");
    }

    public void decode(byte[] bytes){
        synchronized (this){
            this.notify();
        }
        this.mImageBytes = bytes;
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
        while (!exit) {
            try {
                int width = mCameraSize.width;
                int height = mCameraSize.height;

                com.google.zxing.Result rawResult = null;
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(mImageBytes,
                        width, height,
                        mScanRect.top,mScanRect.left,
                        mScanRect.height(),mScanRect.width(),
                        true);

                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {

                    rawResult = decodeInternal(bitmap);
                } catch (ReaderException re) {
                    // continue
                } catch (NullPointerException npe) {
                    // This is terrible
                } catch (ArrayIndexOutOfBoundsException aoe) {
                    //
                } finally {
                    reset();
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

    public void setFormats(List<String> formats){
        ArrayList<Reader> readers = new ArrayList<>();
        for(int i = 0;i< formats.size();i++){
            String item = formats.get(i);
            switch (item){
                case "ScanType.CODABAR":
                    readers.add(new CodaBarReader());
                    break;
                case "ScanType.QR_CODE":
                    readers.add(new QRCodeReader());
                    break;
                case "ScanType.AZTEC":
                    readers.add(new AztecReader());
                    break;
                case "ScanType.CODE_39":
                    readers.add(new Code39Reader());
                    break;
                case "ScanType.CODE_93":
                    readers.add(new Code93Reader());
                    break;
                case "ScanType.CODE_128":
                    readers.add(new Code128Reader());
                    break;
                case "ScanType.EAN8":
                    readers.add(new EAN8Reader());
                    break;
                case "ScanType.EAN13":
                    readers.add(new EAN13Reader());
                    break;
                case "ScanType.ITF":
                    readers.add(new ITFReader());
                    break;
                case "ScanType.DATA_MATRIX":
                    readers.add(new DataMatrixReader());
                    break;
                case "ScanType.PDF_417":
                    readers.add(new PDF417Reader());
                    break;
            }
        }
        this.mReaders = readers.toArray(new Reader[readers.size()]);

    }

    public  void release(){
        synchronized (this) {
            exit = true;
            mImageBytes = null;
            mReaders = null;
            this.notify();//等待下一次解码
        }
    }

    private void reset() {
        if (mReaders != null) {
            for (Reader reader : mReaders) {
                reader.reset();
            }
        }
    }

    private Result decodeInternal(BinaryBitmap image) throws NotFoundException {
        if (mReaders != null) {
            for (Reader reader : mReaders) {
                try {
                    return reader.decode(image, mHints);
                } catch (ReaderException re) {
                    // continue
                }
            }
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
