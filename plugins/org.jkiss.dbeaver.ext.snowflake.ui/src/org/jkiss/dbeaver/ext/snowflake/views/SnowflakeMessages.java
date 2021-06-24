package org.jkiss.dbeaver.ext.snowflake.views;

import org.eclipse.osgi.util.NLS;

public class SnowflakeMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.snowflake.views.SnowflakeResources"; //$NON-NLS-1$
	public static String label_authenticator;
	public static String label_click_on_test_connection;
	public static String label_connection;
	public static String label_database;
	public static String label_host;
	public static String label_password;
	public static String label_port;
	public static String label_role;
	public static String label_schema;
	public static String label_security;
	public static String label_user;
	public static String label_warehouse;

	public static String dialog_setting_sql;
	public static String dialog_setting_sql_dd_label;
	public static String dialog_setting_sql_dd_string;
	public static String dialog_setting_sql_dd_code_block;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SnowflakeMessages.class);
	}

	private SnowflakeMessages() {
	}
}
