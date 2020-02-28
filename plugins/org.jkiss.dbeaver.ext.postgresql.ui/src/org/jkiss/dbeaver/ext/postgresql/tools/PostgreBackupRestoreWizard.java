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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.ui.IExportWizard;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreBackupRestoreSettings;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupRestoreInfo;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractImportExportWizard;

import java.util.Collection;

abstract class PostgreBackupRestoreWizard<SETTINGS extends PostgreBackupRestoreSettings, PROCESS_ARG extends PostgreDatabaseBackupRestoreInfo>
    extends AbstractImportExportWizard<SETTINGS, PROCESS_ARG> implements IExportWizard {

    PostgreBackupRestoreWizard(DBTTask task) {
        super(task);
    }

    PostgreBackupRestoreWizard(Collection<DBSObject> objects, String title) {
        super(objects, title);
    }

    @Override
    public boolean isVerbose()
    {
        return true;
    }

}
