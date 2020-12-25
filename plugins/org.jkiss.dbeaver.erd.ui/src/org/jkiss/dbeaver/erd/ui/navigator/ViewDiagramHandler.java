/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorEmbedded;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class ViewDiagramHandler extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            for (Object item : ((IStructuredSelection) selection).toList()) {
                DBSObject object = GeneralUtils.adapt(item, DBSEntity.class);
                if (object == null) {
                    object = GeneralUtils.adapt(item, DBSObjectContainer.class);
                }
                if (object != null) {
                    DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject(object);
                    if (node != null) {
                        NavigatorHandlerObjectOpen.openEntityEditor(
                            node,
                            ERDEditorEmbedded.class.getName(),
                            null,
                            null,
                            UIUtils.getActiveWorkbenchWindow(),
                            true);
                    }
                }
            }
        }
        return null;
    }

}