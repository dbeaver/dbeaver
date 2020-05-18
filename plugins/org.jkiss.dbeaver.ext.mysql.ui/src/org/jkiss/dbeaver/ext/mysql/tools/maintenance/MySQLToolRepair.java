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
package org.jkiss.dbeaver.ext.mysql.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table repair
 */
public class MySQLToolRepair implements IUserInterfaceTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<MySQLTable> tables = CommonUtils.filterCollection(objects, MySQLTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Button quickCheck;
        private Button extendedCheck;
        private Button frmCheck;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<MySQLTable> selectedTables)
        {
            super(partSite, "Repair table(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, MySQLTable object) {
            String sql = "REPAIR TABLE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);
            if (quickCheck.getSelection()) sql += " QUICK";
            if (extendedCheck.getSelection()) sql += " EXTENDED";
            if (frmCheck.getSelection()) sql += " USE_FRM";
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            quickCheck = UIUtils.createCheckbox(optionsGroup, "Quick", false);
            quickCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            extendedCheck = UIUtils.createCheckbox(optionsGroup, "Extended", false);
            extendedCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            frmCheck = UIUtils.createCheckbox(optionsGroup, "Use FRM", false);
            frmCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
