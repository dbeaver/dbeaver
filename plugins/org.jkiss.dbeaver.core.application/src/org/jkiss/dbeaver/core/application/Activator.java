package org.jkiss.dbeaver.core.application;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.core.application.rpc.DBeaverInstanceServer;
import org.jkiss.dbeaver.core.application.rpc.IInstanceController;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.core.application";

	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}

	public void start(BundleContext bundleContext) throws Exception {
		instance = this;
		super.start(bundleContext);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		instance = null;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

}
