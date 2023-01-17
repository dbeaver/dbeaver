/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.import_config;

import org.eclipse.osgi.util.NLS;

public class ImportConfigMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.import_config.ImportConfigMessages"; //$NON-NLS-1$
	public static String config_import_wizard_header_import_configuration;
	public static String config_import_wizard_page_caption_connections;
	public static String config_import_wizard_page_dbvis_label_installation_not_found;
	public static String config_import_wizard_page_label_connection_list;
	public static String config_import_wizard_page_squirrel_label_installation_not_found;
	public static String config_import_wizard_page_th_driver;
	public static String config_import_wizard_page_th_name;
	public static String config_import_wizard_page_th_url;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ImportConfigMessages.class);
	}

	private ImportConfigMessages() {
	}
}
