package org.jkiss.dbeaver.ext.iotdb.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class IoTDBUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.iotdb.ui";

    private static IoTDBUIActivator plugin;

    public IoTDBUIActivator() {
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

    public static IoTDBUIActivator getDefault() {
        return plugin;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
