package org.jkiss.dbeaver.ext.starrocks.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class StarRocksActivator extends Plugin {

    private static StarRocksActivator plugin;

    public StarRocksActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static StarRocksActivator getDefault() {
        return plugin;
    }
}