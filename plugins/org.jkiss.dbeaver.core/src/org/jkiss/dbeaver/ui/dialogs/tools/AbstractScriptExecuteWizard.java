/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.File;
import java.util.Collection;

public abstract class AbstractScriptExecuteWizard<BASE_OBJECT extends DBSObject, PROCESS_ARG>
        extends AbstractToolWizard<BASE_OBJECT, PROCESS_ARG> implements IImportWizard
{
    protected File inputFile;

    public AbstractScriptExecuteWizard(Collection<BASE_OBJECT> dbObject, String task) {
        super(dbObject, task);
        this.inputFile = null;
	}

    public File getInputFile()
    {
        return inputFile;
    }

    public void setInputFile(File inputFile)
    {
        this.inputFile = inputFile;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(task);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(logPage);
    }

	@Override
	public void onSuccess(long workTime) {
        UIUtils.showMessageBox(getShell(),
                task,
                NLS.bind(CoreMessages.tools_script_execute_wizard_task_completed, task, getObjectsName()) , //$NON-NLS-1$
                        SWT.ICON_INFORMATION);
	}

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process)
    {
        logPage.startLogReader(
            processBuilder,
            process.getInputStream());
        new TextFileTransformerJob(monitor, inputFile, process.getOutputStream(), getInputCharset(), getOutputCharset()).start();
    }

    @Override
    protected boolean isMergeProcessStreams()
    {
        return true;
    }

    protected String getInputCharset() {
        return GeneralUtils.UTF8_ENCODING;
    }

    protected String getOutputCharset() {
        return GeneralUtils.UTF8_ENCODING;
    }

}
