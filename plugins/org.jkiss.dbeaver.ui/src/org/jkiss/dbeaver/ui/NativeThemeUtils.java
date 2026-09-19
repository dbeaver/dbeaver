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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * This is a hack for SWT Light/Dark theme switch.
 * It relies on Windows native controls theme update.
 * TODO: remove when it will be fixed in Eclipse SWT
 */
public final class NativeThemeUtils {
    private static final Log log = Log.getLog(NativeThemeUtils.class);

    public static void updateNativeTheme(@NotNull Control control) {
        if (!RuntimeUtils.isWindows()) {
            return;
        }
        try {
            Class<?> themeUtils = FrameworkUtil.getBundle(Control.class).loadClass(
                "org.eclipse.swt.widgets.SWTThemeUtils");
            themeUtils.getMethod("updateExplorerTheme", Control.class, boolean.class)
                .invoke(null, control, UIStyles.isDarkTheme());
        } catch (ReflectiveOperationException e) {
            log.debug("Error updating native control theme", e);
        }
    }
}
