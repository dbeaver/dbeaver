package org.jkiss.dbeaver.ext.spanner.ui.internal;

import org.eclipse.osgi.util.NLS;

public class SpannerMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.spanner.ui.internal.messages"; //$NON-NLS-1$

	public static String label_connection;
	public static String label_private_key_path;
	public static String label_project;
	public static String label_instance;
	public static String label_database;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, SpannerMessages.class);
	}

	private SpannerMessages() {
	}
}
