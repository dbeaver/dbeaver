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
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompareObjectsHandler extends AbstractHandler {

    private static final Log log = Log.getLog(CompareObjectsHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection ss = (IStructuredSelection)selection;
        if (ss.size() < 2) {
            log.error("At least 2 objects must be selected to perform compare");
            return null;
        }
        List<DBNDatabaseNode> nodes = new ArrayList<>();
        Class<?> firstType = null;
        DBXTreeNode firstMeta = null;
        for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
            DBNDatabaseNode node = (DBNDatabaseNode) iter.next();
            DBXTreeNode meta = null;
            if (node instanceof DBNDatabaseFolder) {
                meta = node.getMeta();
                if (firstMeta == null) {
                    firstMeta = meta;
                }
            }
            if (node.getObject() == null) {
                log.error("Bad node with null object");
                return null;
            }
            Class<?> itemType = node.getObject().getClass();
            if (firstType == null) {
                firstType = itemType;
            } else {
                if (firstType != itemType || firstMeta != meta) {
                    DBWorkbench.getPlatformUI().showError("Different object types", "Objects of different types were selected. You may compare only objects of the same type");
                    return null;
                }
            }
            nodes.add(node);
        }
        CompareWizardDialog dialog = new CompareWizardDialog(
            workbenchWindow,
            new CompareObjectsWizard(nodes));
        dialog.open();

        return null;
    }
}