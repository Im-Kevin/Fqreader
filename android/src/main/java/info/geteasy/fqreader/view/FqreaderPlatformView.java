package info.geteasy.fqreader.view;
import android.app.ActionBar;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.common.PluginRegistry;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;


public class FqreaderPlatformView implements PlatformView, MethodChannel.MethodCallHandler, ZBarScannerView.ResultHandler {

    private final MethodChannel mMethodChannel;
    private final Context mContext;
    private Map<String, Object> mParams;
    private PluginRegistry.Registrar mRegistrar;
    private ZBarView view;

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void dispose() {
        mParams = null;
        mRegistrar = null;
    }


    public FqreaderPlatformView(Context context, PluginRegistry.Registrar registrar, int id, Map<String, Object> params) {
        this.mContext = context;
        this.mParams = params;
        this.mRegistrar = registrar;
        int width = (int) params.get("width");
        int height = (int) params.get("height");
        int scanTop = (int) params.get("scanTop");
        int scanLeft = (int) params.get("scanLeft");
        int scanRight = (int) params.get("scanRight");
        int scanBottom = (int) params.get("scanBottom");
        view = new ZBarView(context);
        view.setResultHandler(this);
        view.setAutoFocus(true);

        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(width, height);
        view.setLayoutParams(layoutParams);

        Rect scanRect = new Rect(scanLeft, scanTop, scanRight,scanBottom);
        view.setFramingRectInPreview(scanRect);

        // 操作监听
        mMethodChannel = new MethodChannel(registrar.messenger(), "info.geteasy.fqreader_view.event" + id);
        mMethodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "getFlash":
                result.success(view.getFlash());
                break;
            case "startCamera":
                view.startCamera();
                break;
            case "stopCamera":
                view.stopCamera();
                break;
            case "setFlash":
                boolean flag = methodCall.argument("flag");
                view.setFlash(flag);
                break;

        }

    }
    @Override
    public void handleResult(Result rawResult) {
        HashMap<String, Object> rest = new HashMap<String, Object>();
        rest.put("data", rawResult.getContents());
        mMethodChannel.invokeMethod("onScan", rest);
    }
}
