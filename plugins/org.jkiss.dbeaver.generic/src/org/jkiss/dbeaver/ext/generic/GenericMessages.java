/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic;

import org.eclipse.osgi.util.NLS;

public class GenericMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.generic.GenericResources"; //$NON-NLS-1$


	public static String dialog_connection_advanced_tab;


	public static String dialog_connection_advanced_tab_tooltip;


	public static String dialog_connection_browse_button;


	public static String dialog_connection_database_schema_label;


	public static String dialog_connection_db_file_chooser_text;


	public static String dialog_connection_db_folder_chooser_message;


	public static String dialog_connection_db_folder_chooser_text;


	public static String dialog_connection_edit_driver_button;


	public static String dialog_connection_general_tab;


	public static String dialog_connection_general_tab_tooltip;


	public static String dialog_connection_host_label;


	public static String dialog_connection_jdbc_url_;


	public static String dialog_connection_password_label;


	public static String dialog_connection_path_label;


	public static String dialog_connection_port_label;


	public static String dialog_connection_server_label;


	public static String dialog_connection_test_connection_button;


	public static String dialog_connection_user_name_label;


	public static String ui_common_button_help;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, GenericMessages.class);
	}

	private GenericMessages() {
	}
}
