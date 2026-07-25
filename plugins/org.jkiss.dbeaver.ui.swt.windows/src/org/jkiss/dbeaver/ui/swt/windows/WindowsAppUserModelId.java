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
package org.jkiss.dbeaver.ui.swt.windows;

import org.eclipse.swt.internal.win32.OS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

/**
 * Sets the Windows Application User Model ID for the current process.
 * <p>
 * Must be called before any SWT {@link org.eclipse.swt.widgets.Display} is created
 * so the taskbar can group the launcher and UI under one pinned icon.
 */
public final class WindowsAppUserModelId {

    private static final Log log = Log.getLog(WindowsAppUserModelId.class);

    /**
     * Stable AUMID for DBeaver Community. Must not contain spaces
     * (see Microsoft AppUserModelID rules).
     */
    public static final String CE_APP_ID = "DBeaverCorp.DBeaverCE";

    private WindowsAppUserModelId() {
    }

    /**
     * Assigns an explicit AppUserModelID to the current process.
     *
     * @param appUserModelId Pascal-cased ID without spaces, e.g. {@link #CE_APP_ID}
     * @return {@code true} if the Win32 call succeeded
     */
    public static boolean setCurrentProcessId(@NotNull String appUserModelId) {
        // Win32 PCWSTR requires a trailing null; String.toCharArray() does not add one.
        char[] buffer = new char[appUserModelId.length() + 1];
        appUserModelId.getChars(0, appUserModelId.length(), buffer, 0);
        int hr = OS.SetCurrentProcessExplicitAppUserModelID(buffer);
        if (hr != OS.S_OK) {
            log.debug("SetCurrentProcessExplicitAppUserModelID failed, HRESULT=" + hr);
            return false;
        }
        return true;
    }
}
