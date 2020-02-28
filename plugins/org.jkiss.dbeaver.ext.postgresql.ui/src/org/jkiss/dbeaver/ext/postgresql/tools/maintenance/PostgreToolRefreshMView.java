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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreMaterializedView;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table analyze
 */
public class PostgreToolRefreshMView implements IUserInterfaceTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<PostgreMaterializedView> tables = CommonUtils.filterCollection(objects, PostgreMaterializedView.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Button withDataCheck;

        public SQLDialog(IWorkbenchPartSite partSite, List<PostgreMaterializedView> selectedTables)
        {
            super(partSite, PostgreMessages.tool_refresh_mview_title_table, selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            String sql = "REFRESH MATERIALIZED VIEW " + ((PostgreMaterializedView) object).getFullyQualifiedName(DBPEvaluationContext.DDL) + " ";
            if (withDataCheck.getSelection()) {
                sql += "WITH DATA";
            } else {
                sql += "WITH NO DATA";
            }
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {

            Group optionsGroup = UIUtils.createControlGroup(parent, PostgreMessages.tool_refresh_mview_group_option, 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            withDataCheck = UIUtils.createCheckbox(optionsGroup,  PostgreMessages.tool_refresh_mview_with_data, PostgreMessages.tool_refresh_mview_with_data_tooltip, true, 0);
            withDataCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
