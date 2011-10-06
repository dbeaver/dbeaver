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

	public static String advanced;
	public static String advanced_custom_driver_properties_tooltip;
	public static String basic;
	public static String browse___;
	public static String connection_type;
	public static String custom;
	public static String general;
	public static String general_connection_properties;
	public static String host;
	public static String jdbc_url;
	public static String oracle_home;
	public static String os_authentication;
	public static String password;
	public static String port;
	public static String role;
	public static String security;
	public static String select_oracle_home;
	public static String sid_service;
	public static String test_connection____;
	public static String tns;
	public static String user_name;
	public static String v_;
}
