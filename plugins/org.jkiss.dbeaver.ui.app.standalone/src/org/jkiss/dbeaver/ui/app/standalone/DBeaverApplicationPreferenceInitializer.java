/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Overwrites IDEPreferenceInitializer behavior.
 */
public class DBeaverApplicationPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		IEclipsePreferences node= DefaultScope.INSTANCE.getNode(IDEWorkbenchPlugin.getDefault().getBundle().getSymbolicName());

		node.putBoolean(IDEInternalPreferences.SHOW_LOCATION, false);
		node.putBoolean(IDEInternalPreferences.SHOW_LOCATION_NAME, false);
		node.putBoolean(IDEInternalPreferences.SHOW_PERSPECTIVE_IN_TITLE, false);
		node.putBoolean(IDEInternalPreferences.SHOW_PRODUCT_IN_TITLE, true);
	}

}
