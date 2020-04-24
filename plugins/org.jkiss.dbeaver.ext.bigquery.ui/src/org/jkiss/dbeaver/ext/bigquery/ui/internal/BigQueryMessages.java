package org.jkiss.dbeaver.ext.bigquery.ui.internal;

import org.eclipse.osgi.util.NLS;

public class BigQueryMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.bigquery.ui.internal.messages"; //$NON-NLS-1$
	public static String label_additional_project;
	public static String label_connection;
	public static String label_host;
	public static String label_key_path;
	public static String label_oauth_type;
	public static String label_port;
	public static String label_private_key_path;
	public static String label_project;
	public static String label_security;
	public static String label_server_info;
	public static String label_service_account;
	public static String label_service_based;
	public static String label_user_based;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BigQueryMessages.class);
	}

	private BigQueryMessages() {
	}
}
