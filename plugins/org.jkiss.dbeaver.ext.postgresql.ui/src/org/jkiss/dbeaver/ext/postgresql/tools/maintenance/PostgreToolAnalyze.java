/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreSQLTasks;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table analyze
 */
public class PostgreToolAnalyze implements IUserInterfaceTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException {
        List<PostgreTableBase> tables = CommonUtils.filterCollection(objects, PostgreTableBase.class);
        if (!tables.isEmpty()) {
            TaskConfigurationWizardDialog.openNewTaskDialog(
                    window,
                    NavigatorUtils.getSelectedProject(),
                    PostgreSQLTasks.TASK_TABLE_ANALYZE,
                    new StructuredSelection(objects.toArray()));
        } else {
            List<PostgreDatabase> databases = CommonUtils.filterCollection(objects, PostgreDatabase.class);
            if (!databases.isEmpty()) {
                TaskConfigurationWizardDialog.openNewTaskDialog(
                        window,
                        NavigatorUtils.getSelectedProject(),
                        PostgreSQLTasks.TASK_DATABASE_ANALYZE,
                        new StructuredSelection(objects.toArray()));
            }
        }
    }
}
