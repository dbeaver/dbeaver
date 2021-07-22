package org.jkiss.dbeaver.ext.oceanbase.ui.internal;

import org.eclipse.osgi.util.NLS;

public class OceanbaseMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.oceanbase.ui.internal.OceanbaseMessages"; //$NON-NLS-1$
	public static String oceanbase_connection_page_label_port;
	public static String oceanbase_connection_page_label_host;
	public static String oceanbase_connection_page_label_database;
	public static String oceanbase_connection_page_label_password;
	public static String oceanbase_connection_page_label_user;
	public static String oceanbase_connection_page_label_url;
	public static String oceanbase_connection_page_label_connection;
	public static String oceanbase_connection_page_label_tenant;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, OceanbaseMessages.class);
	}

	private OceanbaseMessages() {
	}
}
