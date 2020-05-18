/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle.ui.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

import java.util.Collection;
import java.util.List;

/**
 * Gather statistics
 */
public class OracleToolGatherStatistics implements IUserInterfaceTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        if (!objects.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), objects);
            dialog.open();
        }
    }

    static class SQLDialog extends OracleMaintenanceDialog<DBSObject> {

        private Spinner samplePercent;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<DBSObject> selectedTables)
        {
            super(partSite, "Gather statistics", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, DBSObject object) {
            if (object instanceof OracleTable) {
                OracleTable table = (OracleTable)object;
                String sql = "BEGIN \n" +
                    " DBMS_STATS.GATHER_TABLE_STATS (\n" +
                    " OWNNAME => '" + DBUtils.getQuotedIdentifier(table.getSchema()) + "',\n" +
                    " TABNAME => '" + DBUtils.getQuotedIdentifier(table) + "',\n" +
                    " estimate_percent => " + samplePercent.getSelection() + "\n" +
                    " );\n" +
                    "END;";
                lines.add(sql);
            } else if (object instanceof OracleTableIndex) {
                OracleTableIndex index = (OracleTableIndex)object;
                String sql = "ALTER INDEX " + index.getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPUTE STATISTICS";
                lines.add(sql);
            }
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            samplePercent = UIUtils.createLabelSpinner(optionsGroup, "Sample Percent", 5, 0, 100);
            samplePercent .addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
