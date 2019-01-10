/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table truncate
 */
public class OracleToolTruncate implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<OracleTable> tables = CommonUtils.filterCollection(objects, OracleTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends OracleMaintenanceDialog<OracleTable> {

        private Button reuseStorage;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<OracleTable> selectedTables)
        {
            super(partSite, "Truncate table(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, OracleTable object) {
            String sql = "TRUNCATE TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
            if (reuseStorage.getSelection()) {
                sql += " REUSE STORAGE";
            }
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            reuseStorage = UIUtils.createCheckbox(optionsGroup, "Reuse storage", false);
            reuseStorage.addSelectionListener(SQL_CHANGE_LISTENER);
            reuseStorage.setToolTipText(
                "Specify REUSE STORAGE to retain the space from the deleted rows allocated to the table.\n" +
                "Storage values are not reset to the values when the table or cluster was created.\n" +
                "This space can subsequently be used only by new data in the table or cluster resulting from insert or update operations.\n" +
                "This clause leaves storage parameters at their current settings.");

            createObjectsSelector(parent);
        }

        @Override
        protected boolean needsRefreshOnFinish() {
            return true;
        }

    }

}
