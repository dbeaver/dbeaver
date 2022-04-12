/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.eula;

import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.prefs.Preferences;

public class EULAUtils {
    private static final Log log = Log.getLog(EULAUtils.class);

    public static final String DBEAVER_EULA = "DBeaver.eula";

    //TODO change hardcoded eula version to something more flexible
    private static final String eulaVersion = "1.0";

    //Only works for packaged version of dbeaver, will not find anything inside development environment
    private static final String EULA_PATH = SystemVariablesResolver.getInstallPath() + File.separator + "licenses" + File.separator + "dbeaver_license.txt";

    @NotNull
    public static String getEulaVersion() {
        return eulaVersion;
    }

    public static String getPackageEula() {
        String eula;
        try (FileReader reader = new FileReader(EULA_PATH)) {
            eula = IOUtils.readToString(reader);
        } catch (IOException e) {
            log.error("Error reading End-user license agreement file", e);
            return null;
        }
        return eula;
    }


    public static void showEULAIfNeeded(@NotNull Shell shell) {
        Preferences preferences = Preferences.userNodeForPackage(DBWorkbench.getPlatform().getApplication().getClass());
        if (!EULAUtils.getEulaVersion().equals(preferences.get(DBEAVER_EULA, null))) {
            //Used didn't accept EULA before or dialog was shown on different eula version
            preferences.remove(DBEAVER_EULA);
            showEula(shell, true);
        }
    }

    public static void showEula(@NotNull Shell shell, boolean needsConfirmation) {
        String eula = EULAUtils.getPackageEula();
        if (needsConfirmation) {
            showEulaConfirmationDialog(shell, eula);
        } else {
            showEulaInfoDialog(shell, eula);
        }
    }

    private static void showEulaConfirmationDialog(@NotNull Shell shell, @Nullable String eula) {
        EULAConfirmationDialog eulaDialog = new EULAConfirmationDialog(shell, eula);
        eulaDialog.open();
    }

    private static void showEulaInfoDialog(@NotNull Shell shell, @Nullable String eula) {
        EULAInfoDialog eulaDialog = new EULAInfoDialog(shell, eula);
        eulaDialog.open();
    }

}
