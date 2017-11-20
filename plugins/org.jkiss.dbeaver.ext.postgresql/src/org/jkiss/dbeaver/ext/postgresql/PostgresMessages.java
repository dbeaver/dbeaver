package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.osgi.util.NLS;

public class PostgresMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.PostgresResources"; //$NON-NLS-1$
	
	/*backup wizard*/
	public static String wizard_backup_title;
	public static String wizard_backup_msgbox_success_title;
	public static String wizard_backup_msgbox_success_description;	
	public static String wizard_backup_page_object_title;
	public static String wizard_backup_page_object_description;
	public static String wizard_backup_page_object_group_object;
	public static String wizard_backup_page_object_checkbox_show_view;	
	public static String wizard_backup_page_setting_title;
	public static String wizard_backup_page_setting_description;
	public static String wizard_backup_page_setting_group_setting;
	public static String wizard_backup_page_setting_label_format;
	public static String wizard_backup_page_setting_label_compression;
	public static String wizard_backup_page_setting_label_encoding;
	public static String wizard_backup_page_setting_checkbox_use_insert;
	public static String wizard_backup_page_setting_group_output;
	public static String wizard_backup_page_setting_label_output_folder;
	public static String wizard_backup_page_setting_label_file_name_pattern;
	public static String wizard_backup_page_setting_label_file_name_pattern_output;
	public static String wizard_backup_page_setting_group_security;
	public static String wizard_backup_page_setting_group_security_label_info;
	public static String wizard_backup_page_setting_group_security_btn_authentication;
	public static String wizard_backup_page_setting_group_security_btn_reset_default;

	
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PostgresMessages.class);
	}

	private PostgresMessages() {
	}
	
}
