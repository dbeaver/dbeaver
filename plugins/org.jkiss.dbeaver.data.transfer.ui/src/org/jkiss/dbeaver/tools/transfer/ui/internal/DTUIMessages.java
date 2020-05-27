/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tools.transfer.ui.internal;

import org.eclipse.osgi.util.NLS;

public class DTUIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages"; //$NON-NLS-1$

	public static String data_transfer_wizard_name;
	public static String data_transfer_wizard_final_column_source;
	public static String data_transfer_wizard_final_column_source_container;
	public static String data_transfer_wizard_final_column_target;
	public static String data_transfer_wizard_final_column_target_container;
	public static String data_transfer_wizard_final_description;
	public static String data_transfer_wizard_final_group_tables;
	public static String data_transfer_wizard_final_group_objects;
	public static String data_transfer_wizard_final_group_settings_source;
	public static String data_transfer_wizard_final_group_settings_target;
	public static String data_transfer_wizard_final_name;
	public static String data_transfer_wizard_final_title;


	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DTUIMessages.class);
	}

	private DTUIMessages() {
	}
}
