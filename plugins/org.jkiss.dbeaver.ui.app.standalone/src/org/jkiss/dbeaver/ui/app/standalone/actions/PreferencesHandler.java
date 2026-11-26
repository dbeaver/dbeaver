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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.dialogs.WizardDialogModeless;

public class PreferencesHandler extends AbstractHandler {

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        try {
            WizardDialogModeless.showPage(
                HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell(),
                event.getParameter(IWorkbenchCommandConstants.WINDOW_PREFERENCES_PARM_PAGEID)
            );
        } catch (DBException e) {
            throw new ExecutionException("Unable to open preferences", e);
        }
        return null;
    }
}
