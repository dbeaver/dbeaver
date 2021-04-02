package org.jkiss.dbeaver.ext.denodo.views;

import org.eclipse.osgi.util.NLS;

public class DenodoMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.denodo.views.DenodoResources"; //$NON-NLS-1$
	public static String label_click_on_test_connection;
	public static String label_connection;
	public static String label_database;
	public static String label_host;
	public static String label_password;
	public static String label_port;
	public static String label_security;
	public static String label_user;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DenodoMessages.class);
	}

	private DenodoMessages() {
	}
}
