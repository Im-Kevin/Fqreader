package info.geteasy.fqreader;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.oned.CodaBarReader;
import com.google.zxing.oned.Code128Reader;
import com.google.zxing.oned.Code39Reader;
import com.google.zxing.oned.Code93Reader;
import com.google.zxing.oned.EAN13Reader;
import com.google.zxing.oned.EAN8Reader;
import com.google.zxing.oned.ITFReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class MultipleDecode implements Reader {
    private Reader[] mReaders;
    private Map<DecodeHintType, ?> mHints;

    void setFormats(List<String> formats){
        ArrayList<Reader> readers = new ArrayList<>();
        if(formats.contains("ScanType.ALL")){
            readers.add(new CodaBarReader());
            readers.add(new QRCodeReader());
            readers.add(new AztecReader());
            readers.add(new Code39Reader());
            readers.add(new Code93Reader());
            readers.add(new Code128Reader());
            readers.add(new EAN8Reader());
            readers.add(new EAN13Reader());
            readers.add(new ITFReader());
            readers.add(new DataMatrixReader());
            readers.add(new PDF417Reader());
        }else{
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
        }
        this.mReaders = readers.toArray(new Reader[readers.size()]);

    }

    void setHints(Map<DecodeHintType, ?> hints){
        this.mHints = hints;
    }

    @Override
    public Result decode(BinaryBitmap image) throws NotFoundException{
        return decodeInternal(image);
    }

    @Override
    public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException{
        setHints(hints);
        return decodeInternal(image);
    }

    @Override
    public void reset() {
        if (mReaders != null) {
            for (Reader reader : mReaders) {
                reader.reset();
            }
        }
    }

    public Map<String,String> toFlutterMap(Result result){
        Map<String,String> mapResult = new HashMap<>();
        mapResult.put("data",result.getText());
        BarcodeFormat format = result.getBarcodeFormat();
        switch(format){
            case QR_CODE:
                mapResult.put("scanType","ScanType.QR_CODE");
                break;
            case AZTEC:
                mapResult.put("scanType","ScanType.AZTEC");
                break;
            case CODABAR:
                mapResult.put("scanType","ScanType.CODABAR");
                break;
            case CODE_39:
                mapResult.put("scanType","ScanType.CODE_39");
                break;
            case CODE_93:
                mapResult.put("scanType","ScanType.CODE_93");
                break;
            case CODE_128:
                mapResult.put("scanType","ScanType.CODE_128");
                break;
            case EAN_8:
                mapResult.put("scanType","ScanType.EAN8");
                break;
            case EAN_13:
                mapResult.put("scanType","ScanType.EAN13");
                break;
            case ITF:
                mapResult.put("scanType","ScanType.ITF");
                break;
            case DATA_MATRIX:
                mapResult.put("scanType","ScanType.DATA_MATRIX");
                break;
            case PDF_417:
                mapResult.put("scanType","ScanType.PDF_417");
                break;
        }
        return mapResult;
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
