/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.runtime.TasksJob;

import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerSetActiveObject extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection) selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNDatabaseNode) {
                final DBNDatabaseNode databaseNode = (DBNDatabaseNode)element;
                final DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                    DBSObjectSelector.class, databaseNode.getObject());
                if (activeContainer != null) {
                    TasksJob.runTask("Select active object", new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                            try {
                                activeContainer.setDefaultObject(monitor, databaseNode.getObject());
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                }
            }
        }
        return null;
    }
}
