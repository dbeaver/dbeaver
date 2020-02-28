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
package org.jkiss.dbeaver.ext.oracle.ui.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleMaterializedView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table MView refresh
 */
public class OracleToolRefreshMView implements IUserInterfaceTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<OracleMaterializedView> tables = CommonUtils.filterCollection(objects, OracleMaterializedView.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends OracleMaintenanceDialog<OracleMaterializedView> {

        private Button methodFast;
        private Button methodForce;
        private Button methodComplete;
        private Button methodAlways;
        private Button methodRecomputePartitions;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<OracleMaterializedView> selectedTables)
        {
            super(partSite, "Refresh materialized view(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, OracleMaterializedView object) {
            String method = "";
            if (methodFast.getSelection()) method += "f";
            if (methodForce.getSelection()) method += "?";
            if (methodComplete.getSelection()) method += "c";
            if (methodAlways.getSelection()) method += "a";
            if (methodRecomputePartitions.getSelection()) method += "p";

            StringBuilder sql = new StringBuilder();
            sql.append("CALL DBMS_MVIEW.REFRESH('").append(object.getFullyQualifiedName(DBPEvaluationContext.DDL)).append("',");
            sql.append("'").append(method).append("'");
            sql.append(")");
            lines.add(sql.toString());
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            methodFast = UIUtils.createCheckbox(optionsGroup, "Fast", "Fast refresh", false, 1);
            methodFast.addSelectionListener(SQL_CHANGE_LISTENER);
            methodForce = UIUtils.createCheckbox(optionsGroup, "Force", "Force refresh", false, 1);
            methodForce.addSelectionListener(SQL_CHANGE_LISTENER);
            methodComplete = UIUtils.createCheckbox(optionsGroup, "Complete", "Complete refresh", false, 1);
            methodComplete.addSelectionListener(SQL_CHANGE_LISTENER);
            methodAlways = UIUtils.createCheckbox(optionsGroup, "Always", "Always refresh", false, 1);
            methodAlways.addSelectionListener(SQL_CHANGE_LISTENER);
            methodRecomputePartitions = UIUtils.createCheckbox(optionsGroup, "Recompute partitions", "Recompute the rows in the materialized view affected by changed partitions in the detail tables", false, 1);
            methodRecomputePartitions.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }

        @Override
        protected boolean needsRefreshOnFinish() {
            return true;
        }

    }

}
