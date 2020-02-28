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
package org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObject;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class SQLServerToolTriggerToggle implements IUserInterfaceTool {

    private static final Log log = Log.getLog(SQLServerToolTriggerToggle.class);

    private boolean isEnable;

    SQLServerToolTriggerToggle(boolean enable) {
        this.isEnable = enable;
    }

    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) {
        List<SQLServerTableTrigger> triggeList = CommonUtils.filterCollection(objects, SQLServerTableTrigger.class);
        if (!triggeList.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), triggeList);
            dialog.open();
        }
    }

    class SQLDialog extends TableToolDialog {

        SQLDialog(IWorkbenchPartSite partSite, List<SQLServerTableTrigger> selectedTrigger) {
            super(partSite, (isEnable ? "Enable" : "Disable") + " trigger", selectedTrigger);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, SQLServerObject object) {
            lines.add("ALTER TABLE " + ((SQLServerTableTrigger) object).getTable() + " "
                + (isEnable ? "ENABLE" : "DISABLE") + " TRIGGER " + DBUtils.getQuotedIdentifier(object) + " ");
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }

        @Override
        protected boolean needsRefreshOnFinish() {
            return true;
        }
    }
}
