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

package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.dialogs.ValueViewDialog;

import java.util.Date;

/**
 * DateTimeStandaloneEditor
 */
public class DateTimeStandaloneEditor extends ValueViewDialog {

    private CustomTimeEditor timeEditor;
    private boolean dirty;

    public DateTimeStandaloneEditor(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        IValueController valueController = getValueController();
        Object value = valueController.getValue();

        Composite dialogGroup = (Composite) super.createDialogArea(parent);
        Composite panel = UIUtils.createComposite(dialogGroup, 3);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int style = SWT.NONE;
        if (valueController.isReadOnly()) {
            style |= SWT.READ_ONLY;
        }

        UIUtils.createControlLabel(panel, "Time");
        timeEditor = new CustomTimeEditor(panel, style, false);
        timeEditor.getControl().addListener(SWT.Modify, event -> dirty = true);

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        timeEditor.getControl().setLayoutData(gd);

        primeEditorValue(value);

        Button button = UIUtils.createPushButton(panel, "Set Current", null);

        button.setEnabled(!valueController.isReadOnly());
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                primeEditorValue(new Date());
            }
        });

        return dialogGroup;
    }

    @Override
    public Object extractEditorValue() throws DBException {
        try (DBCSession session = getValueController().getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make datetime value from editor")) {
            final String strValue = timeEditor.getValue();
            return getValueController().getValueHandler().getValueFromObject(session, getValueController().getValueType(), strValue, false, false);
        }
    }

    @Override
    public void primeEditorValue(@Nullable Object value)
    {
        final String strValue = value == null ?
            "" :
            getValueController().getValueHandler().getValueDisplayString(getValueController().getValueType(), value, DBDDisplayFormat.EDIT);
        timeEditor.setValue(strValue);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public Control getControl()
    {
        return null;
    }

}