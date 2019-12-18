/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.io.File;
import java.util.Collection;

public abstract class AbstractImportExportWizard<SETTINGS extends AbstractImportExportSettings<DBSObject>, PROCESS_ARG>
    extends AbstractToolWizard<SETTINGS, DBSObject, PROCESS_ARG> implements IExportWizard {

    public static final String VARIABLE_HOST = "host";
    public static final String VARIABLE_DATABASE = "database";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TIMESTAMP = "timestamp";

    protected AbstractImportExportWizard(Collection<DBSObject> objects, String title) {
        super(objects, title);
    }

    protected AbstractImportExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected boolean isSingleTimeWizard() {
        return false;
    }

    @Override
    public boolean isRunTaskOnFinish() {
        return true;
    }

    public File getOutputFolder() {
        return getSettings().getOutputFolder();
    }

    public void setOutputFolder(File outputFolder) {
        if (outputFolder != null) {
            DialogUtils.setCurDialogFolder(outputFolder.getAbsolutePath());
        }
        this.getSettings().setOutputFolder(outputFolder);
    }

    public String getOutputFilePattern() {
        return getSettings().getOutputFilePattern();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(taskTitle);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(logPage);
    }

    @Override
    public boolean performFinish() {
        if (isExportWizard()) {
            final File dir = getSettings().getOutputFolder();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    logPage.setMessage("Can't create directory '" + dir.getAbsolutePath() + "'", IMessageProvider.ERROR);
                    getContainer().updateMessage();
                    return false;
                }
            }
        }

        return super.performFinish();
    }

    @Override
    public boolean isVerbose() {
        return true;
    }

    public boolean isExportWizard() {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PROCESS_ARG arg, ProcessBuilder processBuilder, Process process) {
        logPage.startLogReader(
            processBuilder,
            process.getErrorStream());
    }

}
