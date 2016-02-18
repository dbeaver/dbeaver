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
package org.jkiss.dbeaver.ext.test.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

public class HandlerConnectionValidate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNDatabaseNode) {
                validateNode(HandlerUtil.getActiveShell(event), ((DBNDatabaseNode)element).getDataSource());
            }
        }
        return null;
    }

    private void validateNode(Shell activeShell, DBPDataSource dataSource) {
        try {
            final DBCSession session = DBUtils.openUtilSession(VoidProgressMonitor.INSTANCE, dataSource, "test");
            final DBCStatement dbStat = session.prepareStatement(DBCStatementType.EXEC, "SELECT x.attributes FROM \"public\".entry x LIMIT 200", true, false, false);
            if (dbStat.executeStatement()) {
                final DBCResultSet dbResult = dbStat.openResultSet();
                while (dbResult.nextRow()) {
                    final Object cellValue = dbResult.getAttributeValue(0);
                    System.out.println(cellValue);
                }
            }

        } catch (DBCException e) {
            e.printStackTrace();
        }
    }


}
