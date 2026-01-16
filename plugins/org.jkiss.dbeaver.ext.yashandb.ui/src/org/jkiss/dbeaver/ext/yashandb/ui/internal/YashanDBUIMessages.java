/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class YashanDBUIMessages extends NLS {

	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages";

	static {
		NLS.initializeMessages(BUNDLE_NAME, YashanDBUIMessages.class);
	}

	private YashanDBUIMessages() {
	}

	/* Schema */
	public static String dialog_schema_edit_title;
	public static String dialog_schema_edit_user_name;
	public static String dialog_schema_edit_user_password;
	public static String dialog_schema_edit_label;

	/* Objects */
	public static String edit_yashandb_foreign_key_manager_dialog_title;
	public static String edit_yashandb_index_manager_dialog_title;
	public static String edit_yashandb_constraint_manager_dialog_title;

	/* session */
    public static String editors_yashandb_session_editor_title_kill_session;
    public static String editors_yashandb_session_editor_action_kill;
    public static String editors_yashandb_session_editor_action__session;
    public static String editors_yashandb_session_editor_confirm_action;
    public static String editors_yashandb_session_editor_confirm_title;
    
    public static String views_session_manager_viewer_show_background;
    public static String views_session_manager_viewer_show_background_tasks_tip;
    public static String views_session_manager_viewer_show_inactive;
    public static String views_session_manager_viewer_show_inactive_sessions_tip;
}
