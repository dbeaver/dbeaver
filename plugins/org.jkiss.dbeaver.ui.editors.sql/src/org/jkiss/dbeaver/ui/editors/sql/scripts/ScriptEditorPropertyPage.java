/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.scripts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;


public class ScriptEditorPropertyPage extends PropertyPage {

    private static final Log log = Log.getLog(ScriptEditorPropertyPage.class);

    private Button chkDisableEditorSvcs; 
    
    public ScriptEditorPropertyPage() {
        super();
    }

    @NotNull
    @Override
    protected Control createContents(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        composite.setLayoutData(data);

        chkDisableEditorSvcs = UIUtils.createCheckbox(composite, SQLEditorMessages.sql_editor_prefs_disable_services_text, false);
        chkDisableEditorSvcs.setSelection(SQLEditorUtils.getDisableEditorServicesProp(getCurrentFile()));
        chkDisableEditorSvcs.setToolTipText(SQLEditorMessages.sql_editor_prefs_disable_services_tip);
        
        return composite;
    }
    
 
    @Override
    protected void performDefaults() {
        super.performDefaults();
        chkDisableEditorSvcs.setSelection(false);
    }
    
    @Override
    public boolean performOk() {
        try {
            SQLEditorUtils.setDisableEditorServicesProp(getCurrentFile(), chkDisableEditorSvcs.getSelection());
        } catch (CoreException e) {
            log.debug(e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    @Nullable
    private IFile getCurrentFile() {
        return super.getElement().getAdapter(IFile.class);
    }
}
