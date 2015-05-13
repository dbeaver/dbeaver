/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.dialogs.BrowseObjectDialog;

public class DataImportHandler extends DataTransferHandler {

    @Override
    protected IDataTransferNode adaptTransferNode(Object object)
    {
        final DBSDataManipulator adapted = RuntimeUtils.getObjectAdapter(object, DBSDataManipulator.class);
        if (adapted != null) {
            return new DatabaseTransferConsumer(adapted);
        } else {
            return null;
        }
    }

    @Override
    protected IDataTransferProducer chooseProducer(ExecutionEvent event, IDataTransferConsumer consumer)
    {
        IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        if (activeProject != null) {
            final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
            final DBNProject rootNode = navigatorModel.getRoot().getProject(activeProject);
            DBNNode node = BrowseObjectDialog.selectObject(
                HandlerUtil.getActiveShell(event),
                "Select source container for '" + consumer.getTargetName() + "'",
                rootNode.getDatabases(),
                null,
                new Class[] {DBSObjectContainer.class, DBSDataContainer.class},
                new Class[] {DBSDataContainer.class});
            if (node instanceof DBNDatabaseNode) {
                DBSObject object = ((DBNDatabaseNode) node).getObject();
                if (object instanceof DBSDataContainer) {
                    return new DatabaseTransferProducer((DBSDataContainer) object);
                }
            }
        }
        return null;
    }
}