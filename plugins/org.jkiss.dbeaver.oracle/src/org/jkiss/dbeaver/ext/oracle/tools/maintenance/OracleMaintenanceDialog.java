/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptProgressListener;
import org.jkiss.dbeaver.ui.dialogs.sql.SQLScriptStatusDialog;

import java.util.Collection;

/**
 * Table truncate
 */
public abstract class OracleMaintenanceDialog<T extends DBSObject> extends GenerateMultiSQLDialog<T>
{

    public OracleMaintenanceDialog(IWorkbenchPartSite partSite, String title, Collection<T> objects) {
        super(partSite, title, objects);
    }

    @Override
    protected SQLScriptProgressListener<T> getScriptListener() {
        return new SQLScriptStatusDialog<T>(getShell(), getTitle() + " progress", null) {

            @Override
            protected void createStatusColumns(Tree objectTree) {
                TreeColumn msgColumn = new TreeColumn(objectTree, SWT.NONE);
                msgColumn.setText("Message");
            }

            @Override
            public void processObjectResults(T object, DBCResultSet resultSet) throws DBCException {
            }

            @Override
            public void endObjectProcessing(T object, Exception error) {
                super.endObjectProcessing(object, error);
                TreeItem treeItem = getTreeItem(object);
                if (treeItem != null) {
                    treeItem.setText(1, error == null ? "Done" : error.getMessage());
                }

            }
        };
    }
}
