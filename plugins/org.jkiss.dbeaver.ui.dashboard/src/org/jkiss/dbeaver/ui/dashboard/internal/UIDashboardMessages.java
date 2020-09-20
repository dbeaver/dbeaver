/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dashboard.internal;

import org.eclipse.osgi.util.NLS;

public class UIDashboardMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages"; //$NON-NLS-1$

    public static String pref_page_dashboards_group_common;
    public static String pref_page_dashboards_open_separate_connection_label;

    //add dashboard dialog
    public static String dialog_add_dashboard_dialog_title;
    public static String dialog_add_dashboard_column_name;
    public static String dialog_add_dashboard_column_description;
    public static String dialog_add_dashboard_message_no_more_dashboards_for;
    public static String dialog_add_dashboard_button_manage;
    public static String dialog_add_dashboard_button_cancel;
    public static String dialog_add_dashboard_button_add;

    //select dashboard database dialog
    public static String dialog_dashboard_database_select_title;
    public static String dialog_dashboard_database_select_column_name;
    public static String dialog_dashboard_database_select_column_description;

    //edit dashboard dialog
    public static String dialog_edit_dashboard_title;
    public static String dialog_edit_dashboard_infolabels_predifined_dashboard;
    public static String dialog_edit_dashboard_maininfo;
    public static String dialog_edit_dashboard_maininfo_labels_id;
    public static String dialog_edit_dashboard_maininfo_labels_name;
    public static String dialog_edit_dashboard_maininfo_labels_db;
    public static String dialog_edit_dashboard_maininfo_buttons_select;
    public static String dialog_edit_dashboard_maininfo_labels_description;
    public static String dialog_edit_dashboard_maininfo_combos_datatype;
    public static String dialog_edit_dashboard_maininfo_combos_datatype_tooltip;
    public static String dialog_edit_dashboard_maininfo_combos_calctype;
    public static String dialog_edit_dashboard_maininfo_combos_calctype_tooltip;
    public static String dialog_edit_dashboard_maininfo_combos_valuetype;
    public static String dialog_edit_dashboard_maininfo_combos_valuetype_tooltip;
    public static String dialog_edit_dashboard_maininfo_combos_interval;
    public static String dialog_edit_dashboard_maininfo_combos_interval_tooltip;
    public static String dialog_edit_dashboard_maininfo_combos_fetchtype;
    public static String dialog_edit_dashboard_maininfo_combos_fetchtype_tooltip;
    public static String dialog_edit_dashboard_queries;
    public static String dialog_edit_dashboard_queries_keys;
    public static String dialog_edit_dashboard_queries_labels;
    public static String dialog_edit_dashboard_queries_infolabels_separator;
    public static String dialog_edit_dashboard_rendering;
    public static String dialog_edit_dashboard_rendering_combos_defaultview;
    public static String dialog_edit_dashboard_rendering_combos_defaultview_tooltip;
    public static String dialog_edit_dashboard_rendering_labels_updateperiod;
    public static String dialog_edit_dashboard_rendering_labels_maxitems;

    //dashboard item config dialog
    public static String dialog_dashboard_item_config_title;
    public static String dialog_dashboard_item_config_dashboardinfo;
    public static String dialog_dashboard_item_config_dashboardinfo_labels_name;
    public static String dialog_dashboard_item_config_dashboardinfo_labels_description;
    public static String dialog_dashboard_item_config_dashboardupdate;
    public static String dialog_dashboard_item_config_dashboardupdate_labels_updateperiod;
    public static String dialog_dashboard_item_config_dashboardupdate_labels_maxitems;
    public static String dialog_dashboard_item_config_dashboardview;
    public static String dialog_dashboard_item_config_dashboardview_combos_view;
    public static String dialog_dashboard_item_config_dashboardview_combos_view_tooltip;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_legend;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_legend_tooltip;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_grid;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_grid_tooltip;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_domainaxis;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_domainaxis_tooltip;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis;
    public static String dialog_dashboard_item_config_dashboardview_checkboxes_rangeaxis_tooltip;
    public static String dialog_dashboard_item_config_buttons_configuration;
    public static String dialog_dashboard_item_config_buttons_sqlqueries;
    public static String dialog_dashboard_item_config_buttons_sqlqueries_dash;

    // dashboard item config dialog
    public static String dialog_dashboard_item_view_title;

    // dashboard manager dialog
    public static String dialog_dashboard_manager_title;
    public static String dialog_dashboard_manager_treecolumn_name;
    public static String dialog_dashboard_manager_button_new;
    public static String dialog_dashboard_manager_button_copy;
    public static String dialog_dashboard_manager_button_edit;
    public static String dialog_dashboard_manager_button_delete;
    public static String dialog_dashboard_manager_infolabel_predifined_dashboard;
    public static String dialog_dashboard_manager_shell_delete_title;
    public static String dialog_dashboard_manager_shell_delete_question;

    // dashboard view config dialog
    public static String dialog_dashboard_view_config_title;
    public static String dialog_dashboard_view_config_group_viewcfg;
    public static String dialog_dashboard_view_config_group_viewcfg_checkbox_connect;
    public static String dialog_dashboard_view_config_group_viewcfg_checkbox_connect_tooltip;
    public static String dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn;
    public static String dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn_tooltip;
    public static String dialog_dashboard_view_config_button_manage;

    // dashboard renderer timeseries
    public static String histogram_timeseries_x_axis_label;
    public static String histogram_timeseries_y_axis_label;
    public static String histogram_timeseries_date_axis_label;

    // dashboard item
    public static String dashboard_item_errorlabel_text;

    // dashboard chart composite
    public static String dashboard_chart_composite_menu_manager_text;

    public static String error_dashboard_view_no_connection_title;
    public static String error_dashboard_view_no_connection_msg;
    public static String error_dashboard_view_cannot_open_title;
    public static String error_dashboard_view_cannot_open_msg;

    public static String dashboard_view_status_off;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIDashboardMessages.class);
    }

    private UIDashboardMessages() {
    }
}
