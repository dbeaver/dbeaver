/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.osgi.util.NLS;

public class CoreMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.core.DBeaverResources"; //$NON-NLS-1$

	public static String productCopyright;

	public static String productEmail;

	public static String productName;
	public static String productTitle;
	public static String productSubTitle;

	public static String productWebsite;
	public static String actions_menu_about;
	public static String actions_menu_database;
	public static String actions_menu_edit;
	public static String actions_menu_exit_emergency;
	public static String actions_menu_file;
	public static String actions_menu_help;
	public static String actions_menu_navigate;
	public static String actions_menu_window;
	public static String DBeaverCore_error_can_create_temp_dir;
	public static String DBeaverCore_error_can_create_temp_file;

	public static String actions_ContentAssistProposal_label;
	public static String actions_ContentAssistProposal_tooltip;
	public static String actions_ContentAssistProposal_description;

	public static String actions_ContentAssistTip_label;
	public static String actions_ContentAssistTip_tooltip;
	public static String actions_ContentAssistTip_description;

	public static String actions_ContentFormatProposal_label;
	public static String actions_ContentFormatProposal_tooltip;
	public static String actions_ContentFormatProposal_description;

	public static String common_error_sql;

	public static String confirm_exit_title;
	public static String confirm_exit_message;
	public static String confirm_exit_toggleMessage;

	public static String confirm_order_resultset_title;
	public static String confirm_order_resultset_message;
	public static String confirm_order_resultset_toggleMessage;

	public static String confirm_close_resultset_edit_title;
	public static String confirm_close_resultset_edit_message;
	public static String confirm_close_resultset_edit_toggleMessage;

	public static String confirm_disconnect_txn_title;
	public static String confirm_disconnect_txn_message;
	public static String confirm_disconnect_txn_toggleMessage;

	public static String confirm_close_entity_edit_title;
	public static String confirm_close_entity_edit_message;
	public static String confirm_close_entity_edit_toggleMessage;

	public static String confirm_entity_delete_title;
	public static String confirm_entity_delete_message;

	public static String confirm_close_editor_edit_title;
	public static String confirm_close_editor_edit_message;
	public static String confirm_close_editor_edit_toggleMessage;

	public static String confirm_driver_download_title;
	public static String confirm_driver_download_message;
	public static String confirm_driver_download_toggleMessage;

	public static String confirm_entity_reject_title;
	public static String confirm_entity_reject_message;
	public static String confirm_entity_reject_toggleMessage;

	public static String confirm_entity_revert_title;
	public static String confirm_entity_revert_message;
	public static String confirm_entity_revert_toggleMessage;

	public static String dialog_about_font;

	public static String dialog_about_label_version;

	public static String dialog_about_title;

	public static String toolbar_datasource_selector_action_read_databases;
	public static String toolbar_datasource_selector_combo_database_tooltip;
	public static String toolbar_datasource_selector_combo_datasource_tooltip;
	public static String toolbar_datasource_selector_empty;
	public static String toolbar_datasource_selector_error_change_database_message;
	public static String toolbar_datasource_selector_error_change_database_title;
	public static String toolbar_datasource_selector_error_database_not_found;
	public static String toolbar_datasource_selector_error_database_change_not_supported;
	public static String toolbar_datasource_selector_resultset_segment_size;

	public static String ui_common_button_help;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
