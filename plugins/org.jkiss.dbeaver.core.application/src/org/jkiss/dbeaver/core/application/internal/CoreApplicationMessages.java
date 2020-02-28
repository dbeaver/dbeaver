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
package org.jkiss.dbeaver.core.application.internal;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.core.CoreMessages;

public class CoreApplicationMessages extends NLS {
	public static final String BUNDLE_NAME = "org.jkiss.dbeaver.core.application.internal.CoreApplicationMessages"; //$NON-NLS-1$
	
	public static String actions_menu_exit_emergency;
    public static String actions_menu_exit_emergency_message;
	public static String actions_menu_reset_ui_settings_title;
	public static String actions_menu_reset_ui_settings_message;

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreApplicationMessages.class);
	}

	private CoreApplicationMessages() {
	}
}
