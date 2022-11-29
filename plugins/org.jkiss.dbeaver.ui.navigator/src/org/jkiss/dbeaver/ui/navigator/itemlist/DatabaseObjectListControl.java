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
package org.jkiss.dbeaver.ui.navigator.itemlist;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ClipboardData;
import org.jkiss.dbeaver.ui.CopyMode;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;

/**
 * DatabaseObjectListControl
 */
public abstract class DatabaseObjectListControl<OBJECT_TYPE extends DBPObject> extends ObjectListControl<OBJECT_TYPE> {

    @Nullable
    private IWorkbenchSite site;

    protected DatabaseObjectListControl(
        @NotNull Composite parent,
        int style,
        @Nullable IWorkbenchSite site,
        @NotNull IContentProvider contentProvider)
    {
        super(parent, style, contentProvider);
        this.site = site;
        setFitWidth(true);

        createContextMenu();
    }

    @Override
    protected ObjectViewerRenderer createRenderer()
    {
        return new ObjectListRenderer();
    }

    private void createContextMenu() {
        if (site == null) {
            return;
        }
        NavigatorUtils.createContextMenu(site, getItemsViewer(), manager -> {
            IAction copyAction = new Action(WorkbenchMessages.Workbench_copy) {
                @Override
                public void run()
                {
                    ClipboardData clipboardData = new ClipboardData();
                    addClipboardData(CopyMode.DEFAULT, clipboardData);
                    clipboardData.pushToClipboard(getDisplay());
                }
            };
            manager.add(copyAction);
            IAction copyAllAction = new Action(ActionUtils.findCommandName(IActionConstants.CMD_COPY_SPECIAL)) {
                @Override
                public void run()
                {
                    ClipboardData clipboardData = new ClipboardData();
                    addClipboardData(CopyMode.ADVANCED, clipboardData);
                    clipboardData.pushToClipboard(getDisplay());
                }
            };
            manager.add(copyAllAction);

            manager.add(new Separator());
            fillCustomActions(manager);
        });
    }

    private class ObjectListRenderer extends ViewerRenderer {
        @Override
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }
    }

}
