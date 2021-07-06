/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;

import java.util.Collection;

public abstract class AbstractNativeImportExportWizard<SETTINGS extends AbstractImportExportSettings<DBSObject>, PROCESS_ARG>
    extends AbstractNativeToolWizard<SETTINGS, DBSObject, PROCESS_ARG> implements IExportWizard {

    protected AbstractNativeImportExportWizard(Collection<DBSObject> objects, String title) {
        super(objects, title);
    }

    protected AbstractNativeImportExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected boolean isSingleTimeWizard() {
        return false;
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
    public boolean isVerbose() {
        return true;
    }
}
