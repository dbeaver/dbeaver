/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.eclipse.osgi.util.NLS;

public class OracleMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.oracle.OracleResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, OracleMessages.class);
	}

	private OracleMessages() {
	}

	public static String dialog_connection_advanced_tab;
	public static String dialog_connection_advanced_tab_tooltip;
	public static String dialog_connection_basic_tab;
	public static String dialog_connection_browse;
	public static String dialog_connection_connection_type_group;
	public static String dialog_connection_custom_tab;
	public static String dialog_connection_general_tab;
	public static String dialog_connection_general_tab_tooltip;
	public static String dialog_connection_host;
	public static String dialog_connection_ora_home;
	public static String dialog_connection_os_authentication;
	public static String dialog_connection_password;
	public static String dialog_connection_port;
	public static String dialog_connection_role;
	public static String dialog_connection_security_group;
	public static String dialog_connection_select_ora_home_msg;
	public static String dialog_connection_sid_service;
	public static String dialog_connection_test_connection;
	public static String dialog_connection_tns_tab;
	public static String dialog_connection_user_name;
	public static String dialog_connection_ver;
}
