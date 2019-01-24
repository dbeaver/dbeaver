package org.jkiss.dbeaver.ui.perspective;

import org.eclipse.osgi.util.NLS;

public class PerspectiveMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.perspective.messages"; //$NON-NLS-1$
	public static String label_active_service_instance;
	public static String label_choose_catalog;
	public static String label_error_list;
	public static String label_instance;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PerspectiveMessages.class);
	}

	private PerspectiveMessages() {
	}
}
