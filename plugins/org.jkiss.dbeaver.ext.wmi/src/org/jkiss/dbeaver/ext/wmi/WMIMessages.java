/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.wmi;

import org.jkiss.dbeaver.utils.NLS;

public class WMIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.wmi.WMIMessages"; //$NON-NLS-1$
	public static String wmi_connection_page_label_domain;
	public static String wmi_connection_page_label_host;
	public static String wmi_connection_page_label_namespace;
	public static String wmi_connection_page_label_password;
	public static String wmi_connection_page_label_user;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, WMIMessages.class);
	}

	private WMIMessages() {
	}
}
