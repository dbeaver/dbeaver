/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
	public void onSuccess() {
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
