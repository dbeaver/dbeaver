/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
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
            super(partSite, "Vacuum table(s)", selectedTables);
        }

        public SQLDialog(IWorkbenchPartSite partSite, PostgreDatabase database)
        {
            super(partSite, "Vacuum database", database);
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
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            fullCheck = UIUtils.createCheckbox(optionsGroup, "Full", "Selects \"full\" vacuum, which can reclaim more space, but takes much longer and exclusively locks the table.\nThis method also requires extra disk space, since it writes a new copy of the table and doesn't release the old copy until the operation is complete.\nUsually this should only be used when a significant amount of space needs to be reclaimed from within the table.", false, 0);
            fullCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            freezeCheck = UIUtils.createCheckbox(optionsGroup, "Freeze", "Selects aggressive \"freezing\" of tuples. Specifying FREEZE is equivalent to performing VACUUM with the vacuum_freeze_min_age and vacuum_freeze_table_age parameters set to zero.\nAggressive freezing is always performed when the table is rewritten, so this option is redundant when FULL is specified.", false, 0);
            freezeCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            analyzeCheck = UIUtils.createCheckbox(optionsGroup, "Analyze", "Updates statistics used by the planner to determine the most efficient way to execute a query.", false, 0);
            analyzeCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            dpsCheck = UIUtils.createCheckbox(optionsGroup, "Disable page skipping", "Normally, VACUUM will skip pages based on the visibility map.\nPages where all tuples are known to be frozen can always be skipped, and those where all tuples are known to be visible to all transactions may be skipped except when performing an aggressive vacuum.\nFurthermore, except when performing an aggressive vacuum, some pages may be skipped in order to avoid waiting for other sessions to finish using them.\nThis option disables all page-skipping behavior, and is intended to be used only the contents of the visibility map are thought to be suspect, which should happen only if there is a hardware or software issue causing database corruption.", false, 0);
            dpsCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
