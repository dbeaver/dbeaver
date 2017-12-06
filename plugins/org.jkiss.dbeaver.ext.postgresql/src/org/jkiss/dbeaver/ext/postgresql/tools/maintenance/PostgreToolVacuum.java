/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table vacuum
 */
public class PostgreToolVacuum implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<PostgreTableBase> tables = CommonUtils.filterCollection(objects, PostgreTableBase.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        } else {
            List<PostgreDatabase> databases = CommonUtils.filterCollection(objects, PostgreDatabase.class);
            if (!databases.isEmpty()) {
                SQLDialog dialog = new SQLDialog(activePart.getSite(), databases.get(0).getDataSource().getDefaultInstance());
                dialog.open();
            }
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Button fullCheck;
        private Button freezeCheck;
        private Button analyzeCheck;
        private Button dpsCheck;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<PostgreTableBase> selectedTables)
        {
            super(partSite, PostgresMessages.tool_vacuum_title_table, selectedTables);
        }

        public SQLDialog(IWorkbenchPartSite partSite, PostgreDatabase database)
        {
            super(partSite, PostgresMessages.tool_vacuum_title_database, database);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            String sql = "VACUUM (VERBOSE";
            if (fullCheck.getSelection()) sql += ",FULL";
            if (freezeCheck.getSelection()) sql += ",FREEZE";
            if (analyzeCheck.getSelection()) sql += ",ANALYZE";
            if (dpsCheck.getSelection()) sql += ",DISABLE_PAGE_SKIPPING";
            sql += ")";
            if (object instanceof PostgreTableBase) {
                sql += " " + ((PostgreTableBase)object).getFullyQualifiedName(DBPEvaluationContext.DDL);
            }
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, PostgresMessages.tool_vacuum_group_option, 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            fullCheck = UIUtils.createCheckbox(optionsGroup, "Full", PostgresMessages.tool_vacuum_full_check_tooltip, false, 0);
            fullCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            freezeCheck = UIUtils.createCheckbox(optionsGroup, "Freeze", PostgresMessages.tool_vacuum_freeze_check_tooltip, false, 0);
            freezeCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            analyzeCheck = UIUtils.createCheckbox(optionsGroup, "Analyze", PostgresMessages.tool_vacuum_analyze_check_tooltip, false, 0);
            analyzeCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            dpsCheck = UIUtils.createCheckbox(optionsGroup, "Disable page skipping", PostgresMessages.tool_vacuum_dps_check_tooltip, false, 0);
            dpsCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
