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
package org.jkiss.dbeaver.ext.test.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
                validateNode(((DBNDatabaseNode)element).getDataSource());
            }
        }
        return null;
    }

    private void validateNode(DBPDataSource dataSource) {
        try {
            try (final DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataSource, "test")) {
                try (final DBCStatement dbStat = session.prepareStatement(DBCStatementType.EXEC, "SELECT x.attributes FROM \"public\".entry x LIMIT 200", true, false, false)) {
                    if (dbStat.executeStatement()) {
                        try (final DBCResultSet dbResult = dbStat.openResultSet()) {
                            while (dbResult.nextRow()) {
                                final Object cellValue = dbResult.getAttributeValue(0);
                                System.out.println(cellValue);
                            }
                        }
                    }
                }
            }

        } catch (DBCException e) {
            e.printStackTrace();
        }
    }


}
