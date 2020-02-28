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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ViewExceptionDialog extends EditTextDialog {

    private Throwable error;

    public ViewExceptionDialog(Shell parentShell, Throwable error)
    {
        super(parentShell, "Error", error.getMessage());
        this.error = error;
        setReadonly(true);
    }

    @Override
    protected void createControlsBeforeText(Composite composite) {
        UIUtils.createControlLabel(composite, "Message");
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        UIUtils.createControlLabel(composite, "Stacktrace");
        Text stText = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        StringWriter sw = new StringWriter();
        PrintWriter buffer = new PrintWriter(sw, true);
        error.printStackTrace(buffer);
        stText.setText(sw.toString());
        return composite;
    }

}
