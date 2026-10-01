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
package org.eclipse.swt.widgets;

import org.eclipse.swt.internal.win32.OS;

public final class SWTThemeUtils {
    private SWTThemeUtils() {
    }

    public static void updateExplorerTheme(Control control, boolean dark) {
        OS.AllowDarkModeForWindow(control.handle, dark);
        OS.SetWindowTheme(control.handle, Display.EXPLORER, null);
    }
}
