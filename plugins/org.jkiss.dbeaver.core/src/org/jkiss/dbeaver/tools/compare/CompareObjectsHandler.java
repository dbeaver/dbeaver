/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompareObjectsHandler extends AbstractHandler {

    static final Log log = Log.getLog(CompareObjectsHandler.class);

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
                    UIUtils.showErrorDialog(null, "Different object types", "Objects of different types were selected. You may compare only objects of the same type");
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