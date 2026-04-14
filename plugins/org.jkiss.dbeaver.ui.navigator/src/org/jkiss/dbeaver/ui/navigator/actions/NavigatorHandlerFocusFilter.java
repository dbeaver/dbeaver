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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;

public class NavigatorHandlerFocusFilter extends AbstractHandler {
    private static final Log log = Log.getLog(NavigatorHandlerFocusFilter.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        if (workbenchWindow == null) {
            return null;
        }
        final IWorkbenchPage activePage = workbenchWindow.getActivePage();
        if (activePage == null) {
            return null;
        }

        try {
            DatabaseNavigatorView view = (DatabaseNavigatorView) activePage.showView(
                DatabaseNavigatorView.VIEW_ID,
                null,
                IWorkbenchPage.VIEW_ACTIVATE
            );
            UIUtils.asyncExec(() -> {
                if (!view.focusFilterControl(true)) {
                    view.setFocus();
                }
            });
        } catch (PartInitException e) {
            log.debug("Cannot focus Database Navigator filter", e);
        }

        return null;
    }
}
