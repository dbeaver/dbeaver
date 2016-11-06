/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol;


import org.eclipse.osgi.util.NLS;

public class ExasolMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.exasol.ExasolResources"; //$NON-NLS-1$
	public static String dialog_table_tools_options;
	public static String dialog_table_tools_result;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ExasolMessages.class);
    }

    private ExasolMessages() {
    }

    public static String editors_exasol_session_editor_title_kill_session;
    public static String editors_exasol_session_editor_action_kill;
    public static String editors_exasol_session_editor_confirm_action;
    public static String editors_exasol_session_editor_title_kill_session_statement;

    public static String dialog_connection_password;
    public static String dialog_connection_port;
    public static String dialog_connection_user_name;

    public static String dialog_table_tools_progress;
	public static String dialog_table_tools_success_title;
	public static String dialog_table_open_output_directory;
	public static String dialog_table_tools_export_title;
	public static String dialog_table_tools_export_compress;
	public static String dialog_table_tools_column_heading;
	public static String dialog_table_tools_file_template;
	public static String dialog_table_tools_string_sep_mode;
	public static String dialog_table_tools_string_sep;
	public static String dialog_table_tools_column_sep;
	public static String dialog_table_tools_row_sep;
	public static String dialog_table_tools_encoding;
	public static String dialog_table_tools_import_title;
    

}
