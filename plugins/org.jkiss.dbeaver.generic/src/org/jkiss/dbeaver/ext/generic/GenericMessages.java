/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    public static String dialog_connection_driver;
    public static String dialog_connection_jdbc_url_;

    public static String dialog_connection_password_label;

    public static String dialog_connection_path_label;

    public static String dialog_connection_port_label;

    public static String dialog_connection_server_label;

    public static String dialog_connection_test_connection_button;

    public static String dialog_connection_user_name_label;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, GenericMessages.class);
    }
    private GenericMessages() {
    }
}
