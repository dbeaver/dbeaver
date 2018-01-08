package org.jkiss.dbeaver.debug.internal.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DebugCoreActivator implements BundleActivator {

    private static DebugCoreActivator activator;
    private static BundleContext bundleContext;
    
    public static DebugCoreActivator getDefault() {
        return activator;
    }

    static BundleContext getContext() {
        return bundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        activator = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        activator = null;
        bundleContext = null;
    }

}
