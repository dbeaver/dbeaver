package org.jkiss.dbeaver.ext.iotdb.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class IoTDBUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.iotdb.ui.internal.IoTDBUIMessages"; //$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, IoTDBUIMessages.class);
    }

    public static String dialog_connection_advanced_tab;
    public static String dialog_connection_advanced_tab_tooltip;
    public static String dialog_connection_browse_button;
    public static String dialog_connection_browse_button_tip;
    public static String dialog_connection_create_button;
    public static String dialog_connection_create_button_tip;
    public static String dialog_connection_database_schema_label;
    public static String dialog_connection_db_file_chooser_text;
    public static String dialog_connection_db_folder_chooser_message;
    public static String dialog_connection_db_folder_chooser_text;
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

    public static String controls_privilege_table_column_privilege_grant;
    public static String controls_privilege_table_column_privilege_grant_tip;
    public static String controls_privilege_table_column_privilege_name;
    public static String controls_privilege_table_column_privilege_name_tip;
    public static String controls_privilege_table_column_privilege_status;
    public static String controls_privilege_table_column_privilege_status_tip;
    public static String controls_privilege_table_push_button_check_all;
    public static String controls_privilege_table_push_button_clear_all;
    public static String edit_command_grant_privilege_action_grant_privilege;
    public static String edit_command_grant_privilege_action_revoke_privilege;
    public static String edit_command_grant_privilege_name_grant_privilege;
    public static String edit_command_grant_privilege_name_revoke_privilege;
    public static String editors_user_editor_abstract_load_grants;
    public static String editors_user_editor_general_control_dba_privileges;
    public static String editors_user_editor_general_group_login;
    public static String editors_user_editor_general_label_user_name;
    public static String editors_user_editor_general_service_load_catalog_privileges;
    public static String editors_user_editor_privileges_service_load_privileges;
    public static String editors_user_editor_privileges_service_load_tables;

    private IoTDBUIMessages() {
    }
}
