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
package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.ExportSettingsExtension;
import org.jkiss.dbeaver.tasks.ui.nativetool.internal.TaskNativeUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.util.Collection;

public abstract class AbstractNativeExportWizard<SETTINGS extends AbstractImportExportSettings<DBSObject> & ExportSettingsExtension<INFO>, INFO>
    extends AbstractNativeImportExportWizard<SETTINGS, DBSObject> {

    protected AbstractNativeExportWizard(Collection<DBSObject> objects, String title) {
        super(objects, title);
    }

    protected AbstractNativeExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    public boolean performFinish() {
        File dir = getSettings().getOutputFolder();
        if (!dir.exists() && !dir.mkdirs()) {
            logPage.setMessage("Can't create directory '" + dir.getAbsolutePath() + "'", IMessageProvider.ERROR);
            getContainer().updateMessage();
            return false;
        }

        //verify that output files do not yet exist
        SETTINGS settings = getSettings();
        for (INFO info: settings.getExportObjects()) {
            File file = settings.getOutputFile(info);
            if (!file.exists() || file.isDirectory()) {
                continue;
            }
            boolean deleteFile = UIUtils.confirmAction(
                TaskNativeUIMessages.tools_db_export_wizard_file_already_exists_title,
                TaskNativeUIMessages.tools_db_export_wizard_file_already_exists_message
            );
            if (!deleteFile) {
                return false;
            }
            boolean fileDeleted = file.delete();
            if (!fileDeleted) {
                DBWorkbench.getPlatformUI().showError(
                    TaskNativeUIMessages.tools_db_export_wizard_file_have_not_been_deleted_title,
                    TaskNativeUIMessages.tools_db_export_wizard_file_have_not_been_deleted_message
                );
                return false;
            }
        }

        return super.performFinish();
    }
}
