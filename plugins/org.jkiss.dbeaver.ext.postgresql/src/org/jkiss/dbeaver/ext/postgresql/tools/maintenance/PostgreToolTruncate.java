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
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
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
 * Table truncate
 */
public class PostgreToolTruncate implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<PostgreTableBase> tables = CommonUtils.filterCollection(objects, PostgreTableBase.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Button onlyCheck;
        private Button restartIdentityCheck;
        private Button cascadeCheck;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<PostgreTableBase> selectedTables)
        {
            super(partSite, PostgresMessages.tool_truncate_title_table, selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            if (object instanceof PostgreTableBase) {
                String sql = "TRUNCATE TABLE"; //$NON-NLS-1$
                if (onlyCheck.getSelection()) sql += " ONLY"; //$NON-NLS-1$
                sql += " " + ((PostgreTableBase) object).getFullyQualifiedName(DBPEvaluationContext.DDL); //$NON-NLS-1$
                if (restartIdentityCheck.getSelection())
                    sql += " RESTART IDENTITY"; //$NON-NLS-1$
                else
                    sql += " CONTINUE IDENTITY"; //$NON-NLS-1$
                if (cascadeCheck.getSelection())
                    sql += " CASCADE"; //$NON-NLS-1$
                else
                    sql += " RESTRICT"; //$NON-NLS-1$
                lines.add(sql);
            }
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, PostgresMessages.tool_truncate_group_option, 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            onlyCheck = UIUtils.createCheckbox(optionsGroup, PostgresMessages.tool_truncate_checkbox_only, PostgresMessages.tool_truncate_checkbox_only_tooltip, false, 0);
            onlyCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            restartIdentityCheck = UIUtils.createCheckbox(optionsGroup, PostgresMessages.tool_truncate_checkbox_restart, PostgresMessages.tool_truncate_checkbox_restart_tooltip, false, 0);
            restartIdentityCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            cascadeCheck = UIUtils.createCheckbox(optionsGroup, PostgresMessages.tool_truncate_checkbox_cascade, PostgresMessages.tool_truncate_checkbox_cascade_tooltip, false, 0);
            cascadeCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }

        @Override
        protected boolean needsRefreshOnFinish() {
            return true;
        }
    }

}
