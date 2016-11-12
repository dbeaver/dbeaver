/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
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
        super(partSite, title, objects, true);
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
            public void processObjectResults(@NotNull T object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException {
            }

            @Override
            public void endObjectProcessing(@NotNull T object, Exception error) {
                super.endObjectProcessing(object, error);
                TreeItem treeItem = getTreeItem(object);
                if (treeItem != null) {
                    treeItem.setText(1, error == null ? "Done" : error.getMessage());
                }

            }
        };
    }
}
