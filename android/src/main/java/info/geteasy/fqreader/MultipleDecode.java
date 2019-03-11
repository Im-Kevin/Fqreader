package info.geteasy.fqreader;

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
    public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
        return decodeInternal(image);
    }

    @Override
    public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
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
