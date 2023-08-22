// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class MockDataUIActivator extends AbstractUIPlugin
{
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.data.generate.ui";
    private static MockDataUIActivator plugin;
    
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        MockDataUIActivator.plugin = this;
    }
    
    public void stop(final BundleContext context) throws Exception {
        MockDataUIActivator.plugin = null;
        super.stop(context);
    }
    
    public static MockDataUIActivator getDefault() {
        return MockDataUIActivator.plugin;
    }
    
    public static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin("org.jkiss.dbeaver.data.generate.ui", path);
    }
    
    public void saveDialogSettings() {
        super.saveDialogSettings();
    }
}
