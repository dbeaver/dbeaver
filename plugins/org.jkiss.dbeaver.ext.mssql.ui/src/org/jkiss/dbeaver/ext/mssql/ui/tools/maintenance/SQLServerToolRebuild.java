/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import java.util.Collection;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerObject;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTable;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerTableTrigger;
import org.jkiss.dbeaver.ext.mssql.ui.tools.maintenance.TableToolDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

public class SQLServerToolRebuild implements IExternalTool {
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
            throws DBException {
        List<SQLServerTable> tables = CommonUtils.filterCollection(objects, SQLServerTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {
        public SQLDialog(IWorkbenchPartSite partSite, Collection<SQLServerTable> selectedTables) {
            super(partSite, "Rebuild index(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, SQLServerObject object) {
            lines.add("ALTER INDEX ALL ON " + ((SQLServerTable) object).getFullyQualifiedName(DBPEvaluationContext.DDL) + " REBUILD ");
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }

}
