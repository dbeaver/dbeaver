/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.workbench;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;

public class WorkbenchUtils {

    public static boolean isPreferencePageExists(@NotNull String pageId) {
        return PlatformUI.getWorkbench().getPreferenceManager().find(pageId) != null;
    }

    public static void removePreferencePages(@NotNull String ... pageIds) {
        // Remove unneeded pref pages and override font preferences page
        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();

        for (String epp : pageIds) {
            pm.remove(epp);
        }
    }

    public static void movePreferencePages(
        @NotNull String[] pageIds,
        @NotNull String toPage
    ) {
        PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();

        for (String pageId : pageIds)  {
            IPreferenceNode uiPage = pm.remove(pageId);
            if (uiPage != null) {
                pm.addTo(toPage, uiPage);
            }
        }
    }

}
