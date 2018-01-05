package org.jkiss.dbeaver.debug.internal.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DebugCoreActivator implements BundleActivator {

    private static DebugCoreActivator activator;
    private static BundleContext bundleContext;
    
    private ProcedureDebugControllerRegistry procedureControllerRegistry;

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
        procedureControllerRegistry = new ProcedureDebugControllerRegistry();
        procedureControllerRegistry.init();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        procedureControllerRegistry.dispose();
        procedureControllerRegistry = null;

        activator = null;
        bundleContext = null;
    }
    
    public ProcedureDebugControllerRegistry getProcedureControllerRegistry() {
        return procedureControllerRegistry;
    }
    

}
