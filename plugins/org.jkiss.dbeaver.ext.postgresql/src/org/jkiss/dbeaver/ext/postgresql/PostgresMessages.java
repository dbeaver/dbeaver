package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.osgi.util.NLS;

public class PostgresMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.PostgresResources"; //$NON-NLS-1$
		
	/*backup wizard*/
	public static String wizard_backup_title;
	public static String wizard_backup_msgbox_success_title;
	public static String wizard_backup_msgbox_success_description;	
	public static String wizard_backup_page_object_title_schema_table;
	public static String wizard_backup_page_object_title;
	public static String wizard_backup_page_object_description;
	public static String wizard_backup_page_object_group_object;
	public static String wizard_backup_page_object_checkbox_show_view;	
	public static String wizard_backup_page_setting_title_setting;
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

	/* wizard restore*/
	public static String wizard_restore_page_setting_btn_clean_first;
	public static String wizard_restore_page_setting_description;
	public static String wizard_restore_page_setting_label_backup_file;
	public static String wizard_restore_page_setting_label_choose_backup_file;
	public static String wizard_restore_page_setting_label_format;
	public static String wizard_restore_page_setting_label_input;
	public static String wizard_restore_page_setting_label_setting;
	public static String wizard_restore_page_setting_title;	
	public static String wizard_restore_page_setting_title_setting;
	
	/* tool script */
	public static String wizard_script_title_import_db;
	public static String wizard_script_title_execute_script;
	public static String tool_script_title_execute;
	public static String tool_script_title_import;
	public static String tool_script_description_execute;
	public static String tool_script_description_import;
	public static String tool_script_label_input;
	public static String tool_script_label_input_file;
	
	/* tool analyze */
	public static String tool_analyze_title_table;
	public static String tool_analyze_title_database;
	
	/* tool vacuum */
	public static String tool_vacuum_analyze_check_tooltip;
	public static String tool_vacuum_dps_check_tooltip;
	public static String tool_vacuum_freeze_check_tooltip;
	public static String tool_vacuum_full_check_tooltip;
	public static String tool_vacuum_group_option;
	public static String tool_vacuum_title_database;
	public static String tool_vacuum_title_table;
	
	/* tool truncate */
	public static String tool_truncate_checkbox_cascade;
	public static String tool_truncate_checkbox_cascade_tooltip;
	public static String tool_truncate_checkbox_only;
	public static String tool_truncate_checkbox_only_tooltip;
	public static String tool_truncate_checkbox_restart;
	public static String tool_truncate_checkbox_restart_tooltip;
	public static String tool_truncate_group_option;
	public static String tool_truncate_title_table;
	
	/* dialog create db */
	public static String dialog_create_db_group_definition;
	public static String dialog_create_db_group_general;
	public static String dialog_create_db_label_db_name;
	public static String dialog_create_db_label_encoding;
	public static String dialog_create_db_label_owner;
	public static String dialog_create_db_label_tablesapce;
	public static String dialog_create_db_label_template_db;
	public static String dialog_create_db_title;
	
	/* dialog create schema */
	public static String dialog_create_schema_name;
	public static String dialog_create_schema_owner;
	public static String dialog_create_schema_title;
	
	/* PostgresSSLConfigurator */
	public static String dialog_connection_network_postgres_ssl_certificates;
	public static String dialog_connection_network_postgres_ssl_certificates_root;
	public static String dialog_connection_network_postgres_ssl_certificates_ca;
	public static String dialog_connection_network_postgres_ssl_certificates_ssl;
	public static String dialog_connection_network_postgres_ssl_certificates_ssl_key;
	public static String dialog_connection_network_postgres_ssl_advanced;
	public static String dialog_connection_network_postgres_ssl_advanced_ssl_mode;
	public static String dialog_connection_network_postgres_ssl_advanced_ssl_factory;
	
	/* PostgreConnectionPage */
	public static String dialog_setting_connection_host;
	public static String dialog_setting_connection_port;
	public static String dialog_setting_connection_database;
	public static String dialog_setting_connection_user;
	public static String dialog_setting_connection_password;
	public static String dialog_setting_connection_settings;
	public static String dialog_setting_connection_localClient;
	public static String dialog_setting_connection_nondefaultDatabase;
	
	
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PostgresMessages.class);
	}

	private PostgresMessages() {
	}
	
}
