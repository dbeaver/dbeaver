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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table analyze
 */
public class PostgreToolAnalyze implements IExternalTool
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

        public SQLDialog(IWorkbenchPartSite partSite, List<PostgreTableBase> selectedTables)
        {
            super(partSite, PostgreMessages.tool_analyze_title_table, selectedTables);
        }

        public SQLDialog(IWorkbenchPartSite partSite, PostgreDatabase database)
        {
            super(partSite, PostgreMessages.tool_analyze_title_database, database);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            if (object instanceof PostgreTableBase) {
                lines.add("ANALYZE VERBOSE " + ((PostgreTableBase)object).getFullyQualifiedName(DBPEvaluationContext.DDL));
            } else if (object instanceof PostgreDatabase) {
                lines.add("ANALYZE VERBOSE");
            }
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }

}
