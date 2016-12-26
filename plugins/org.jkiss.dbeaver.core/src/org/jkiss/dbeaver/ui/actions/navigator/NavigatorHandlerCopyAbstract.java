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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.ClipboardData;
import org.jkiss.dbeaver.ui.IClipboardSource;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class NavigatorHandlerCopyAbstract extends AbstractHandler implements IElementUpdater {

    public NavigatorHandlerCopyAbstract() {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                copySelection(workbenchWindow, activePart, selection);
            }
        });

        return null;
    }

    private void copySelection(IWorkbenchWindow workbenchWindow, IWorkbenchPart activePart, ISelection selection) {
        ClipboardData clipboardData = new ClipboardData();

        {
            IClipboardSource clipboardSource = activePart.getAdapter(IClipboardSource.class);
            if (clipboardSource != null) {
                clipboardSource.addClipboardData(clipboardData);
            }
        }

        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            List<DBNNode> selectedNodes = new ArrayList<>();
            List<DBPNamedObject> selectedObjects = new ArrayList<>();
            List<String> selectedFiles = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object object = iter.next();
                String objectValue = getObjectDisplayString(object);
                if (objectValue == null) {
                    continue;
                }
                DBNNode node = RuntimeUtils.getObjectAdapter(object, DBNNode.class);
                DBPNamedObject dbObject = null;
                if (node instanceof DBNDatabaseNode) {
                    dbObject = ((DBNDatabaseNode) node).getObject();
                }
                if (dbObject == null) {
                    dbObject = RuntimeUtils.getObjectAdapter(object, DBPNamedObject.class);
                }
                if (node != null) {
                    selectedNodes.add(node);
                }
                if (node instanceof DBNResource && ((DBNResource) node).getResource() instanceof IFile) {
                    final IFile file = (IFile) ((DBNResource) node).getResource();
                    if (file != null) {
                        selectedFiles.add(file.getLocation().makeAbsolute().toFile().getAbsolutePath());
                    }
                }
                if (dbObject != null) {
                    selectedObjects.add(dbObject);
                }
                if (buf.length() > 0) {
                    buf.append(GeneralUtils.getDefaultLineSeparator());
                }
                buf.append(objectValue);
            }
            {
                if (buf.length() > 0 && !clipboardData.hasTransfer(TextTransfer.getInstance())) {
                    clipboardData.addTransfer(TextTransfer.getInstance(), buf.toString());
                }
                if (!selectedNodes.isEmpty() && !clipboardData.hasTransfer(TreeNodeTransfer.getInstance())) {
                    clipboardData.addTransfer(TreeNodeTransfer.getInstance(), selectedNodes);
                }
                if (!selectedObjects.isEmpty() && !clipboardData.hasTransfer(DatabaseObjectTransfer.getInstance())) {
                    clipboardData.addTransfer(DatabaseObjectTransfer.getInstance(), selectedObjects);
                }
                if (!selectedFiles.isEmpty() && !clipboardData.hasTransfer(FileTransfer.getInstance())) {
                    clipboardData.addTransfer(FileTransfer.getInstance(), selectedFiles.toArray(new String[selectedFiles.size()]));
                }
            }
        }
        if (clipboardData.hasData()) {
            clipboardData.pushToClipboard(workbenchWindow.getShell().getDisplay());
            ObjectPropertyTester.firePropertyChange(ObjectPropertyTester.PROP_CAN_PASTE);
        }
    }

    protected abstract String getObjectDisplayString(Object object);

    protected abstract String getSelectionTitle(IStructuredSelection selection);

    @Override
    public void updateElement(UIElement element, Map parameters) {
        if (!NavigatorHandlerObjectBase.updateUI) {
            return;
        }
    }

}