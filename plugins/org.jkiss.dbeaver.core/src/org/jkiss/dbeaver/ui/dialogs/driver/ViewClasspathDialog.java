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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.utils.StandardConstants;

import java.util.StringTokenizer;

/**
 * EditDriverDialog
 */
public class ViewClasspathDialog extends Dialog
{
    public ViewClasspathDialog(Shell shell)
    {
        super(shell);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.dialog_view_classpath_title);

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 400;
        group.setLayoutData(gd);

        {
            ListViewer libsTable = new ListViewer(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            libsTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

            String classPath = System.getProperty(StandardConstants.ENV_JAVA_CLASSPATH); //$NON-NLS-1$
            StringTokenizer st = new StringTokenizer(classPath, ";"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                libsTable.getList().add(st.nextToken());
            }
        }
        return group;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            IDialogConstants.OK_LABEL,
            true);
    }

}