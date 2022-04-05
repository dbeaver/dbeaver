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
package org.jkiss.dbeaver.ext.ui.eula;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class EULAHandler extends AbstractHandler {

    public static void showEula(@NotNull IWorkbenchWindow window, boolean needsConfirmation) {
        String eula = EULAUtils.getPackageEula();
        if (needsConfirmation) {
            showEulaConfirmationDialog(window, eula);
        } else {
            showEulaInfoDialog(window, eula);
        }
    }

    private static void showEulaConfirmationDialog(@NotNull IWorkbenchWindow window, @Nullable String eula) {
        EULAConfirmationDialog eulaDialog = new EULAConfirmationDialog(window.getShell(), eula);
        eulaDialog.open();
    }

    private static void showEulaInfoDialog(@NotNull IWorkbenchWindow window, @Nullable String eula) {
        EULAInfoDialog eulaDialog = new EULAInfoDialog(window.getShell(), eula);
        eulaDialog.open();
    }

    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        showEula(HandlerUtil.getActiveWorkbenchWindow(event), false);
        return null;
    }
}
