/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetSelection;
import org.jkiss.dbeaver.ui.editors.sql.handlers.GenerateSQLContributor;

/**
 * SQLUtilsPropertyTester
 */
public class SQLUtilsPropertyTester extends PropertyTester
{
    //static final Log log = LogFactory.getLog(SQLEditorPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql.util";
    public static final String PROP_CAN_GENERATE = "canGenerate";

    public SQLUtilsPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IWorkbenchPart)) {
            return false;
        }
        IStructuredSelection structuredSelection = GenerateSQLContributor.getSelectionFromPart((IWorkbenchPart)receiver);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return false;
        }
        if (property.equals(PROP_CAN_GENERATE)) {
            if (structuredSelection instanceof ResultSetSelection) {
                // Results
                return true;
            } else {
                // Table
                if (structuredSelection.size() != 1) {
                    return false;
                }
                DBNNode node = RuntimeUtils.getObjectAdapter(structuredSelection.getFirstElement(), DBNNode.class);
                if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getObject() instanceof DBSTable) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}