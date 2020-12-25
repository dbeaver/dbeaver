package org.jkiss.dbeaver.ext.wmi;

import org.eclipse.osgi.util.NLS;

public class WMIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.wmi.WMIMessages"; //$NON-NLS-1$
	public static String wmi_connection_page_label_domain;
	public static String wmi_connection_page_label_host;
	public static String wmi_connection_page_label_namespace;
	public static String wmi_connection_page_label_password;
	public static String wmi_connection_page_label_user;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, WMIMessages.class);
	}

	private WMIMessages() {
	}
}
