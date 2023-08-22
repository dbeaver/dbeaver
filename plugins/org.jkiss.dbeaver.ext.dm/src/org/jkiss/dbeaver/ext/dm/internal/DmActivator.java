package org.jkiss.dbeaver.ext.dm.internal;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.osgi.framework.BundleContext;

public class DmActivator extends Plugin {

	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.dm";
	
	private static DmActivator plugin;
	
	private BundlePreferenceStore preferenceStore;
	
	public DmActivator () {}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	public static DmActivator getDefault() {
		return plugin;
	}
	
	public DBPPreferenceStore getPreferenceStore() {
		if(preferenceStore == null) {
			preferenceStore = new BundlePreferenceStore(getBundle());
		}
		return preferenceStore;
	}
}
