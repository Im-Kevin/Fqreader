package info.geteasy.fqreader.factory;

import android.content.Context;

import java.util.Map;

import info.geteasy.fqreader.view.FqreaderPlatformView;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class ZBarFactory  extends PlatformViewFactory {
    private PluginRegistry.Registrar registrar;

    public ZBarFactory(PluginRegistry.Registrar registrar) {
        super(StandardMessageCodec.INSTANCE);
        this.registrar = registrar;
    }

    @Override
    public PlatformView create(Context context, int id, Object args) {
        Map<String, Object> params = (Map<String, Object>) args;
        return new FqreaderPlatformView(context, registrar, id, params);
    }
}
