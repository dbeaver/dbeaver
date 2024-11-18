package org.jkiss.dbeaver.ext.kingbase;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class KingbaseUIActivator extends AbstractUIPlugin {
    
    public static final String IMG_KINGBASE_SQL = "IMG_KINGBASE_SQL"; 

	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.kingbase"; 

	private static KingbaseUIActivator plugin;
    private static BundleContext bundleContext;

    public KingbaseUIActivator() {
	}

	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		plugin = this;
	}

	@Override
    public void stop(BundleContext context) throws Exception {
		plugin = null;
		bundleContext = context;
		super.stop(context);
	}

	public static KingbaseUIActivator getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	@Override
	protected void initializeImageRegistry(ImageRegistry reg)
	{
	    super.initializeImageRegistry(reg);
	    reg.put(IMG_KINGBASE_SQL, getImageDescriptor("$nl$/icons/kingbase_icon.png")); //$NON-NLS-1$
	}
	
	public IEventBroker getEventBroker() {
        IEclipseContext serviceContext = EclipseContextFactory.getServiceContext(bundleContext);
        return serviceContext.get(IEventBroker.class);
    }
}
