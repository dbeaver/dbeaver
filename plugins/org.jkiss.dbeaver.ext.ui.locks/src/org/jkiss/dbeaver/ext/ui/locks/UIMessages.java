package org.jkiss.dbeaver.ext.ui.locks;

import org.eclipse.osgi.util.NLS;

public class UIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.ui.locks.UIResources"; //$NON-NLS-1$
	public static String actions_refresh_control_kill_waiting_session;
	public static String actions_refresh_control_refresh_locks;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, UIMessages.class);
	}

	private UIMessages() {
	}
}
