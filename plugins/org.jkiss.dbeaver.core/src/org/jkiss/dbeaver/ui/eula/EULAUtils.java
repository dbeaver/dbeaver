/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EULAUtils {
    private static final Log log = Log.getLog(EULAUtils.class);

    public static final String DBEAVER_EULA = "DBeaver.eula";

    //TODO change hardcoded eula version to something more flexible
    private static final String eulaVersion = "1.0";

    @NotNull
    public static String getEulaVersion() {
        return eulaVersion;
    }

    public static String getPackageEula() {
        try {
            Path installPath = RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL());
            Path eulaPath = installPath.resolve("eula.txt");
            if (!Files.exists(eulaPath) && DBWorkbench.getPlatform().getApplication().isCommunity()) {
                eulaPath = installPath.resolve("licenses/dbeaver-community-license.txt");
            }
            if (Files.exists(eulaPath)) {
                return Files.readString(eulaPath);
            }
        } catch (IOException e) {
            log.debug(e);
        }

        return "EULA";
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
