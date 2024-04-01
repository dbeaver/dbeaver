/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.dialogs.ValueViewDialog;

import java.util.Date;

/**
 * DateTimeStandaloneEditor
 */
public class DateTimeStandaloneEditor extends ValueViewDialog {

    private CustomTimeEditor timeEditor;
    private boolean dirty;
    private IValueController valueController;

    public DateTimeStandaloneEditor(IValueController valueController) {
        super(valueController);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        valueController = getValueController();
        Object value = valueController.getValue();
        Composite dialogGroup = super.createDialogArea(parent);
        Composite panel = UIUtils.createComposite(dialogGroup, 3);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        int style = SWT.NONE;
        if (valueController.isReadOnly()) {
            style |= SWT.READ_ONLY;
        }

        timeEditor = new CustomTimeEditor(panel, style, false, false);
        timeEditor.getControl().addListener(SWT.Modify, event -> dirty = true);

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        timeEditor.getControl().setLayoutData(gd);
        timeEditor.createDateFormat(valueController.getValueType());
        timeEditor.setEditable(!valueController.isReadOnly());

        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_USE_DATETIME_EDITOR)){
            timeEditor.setToTextComposite();
        }
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

    private boolean isCalendarMode() {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_USE_DATETIME_EDITOR);
    }

    @Override
    public Object extractEditorValue() throws DBException {
        try (DBCSession session = getValueController().getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make datetime value from editor")) {
            if (!isCalendarMode()) {
                final String strValue = timeEditor.getValueAsString();
                return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(), strValue, false, true);
            } else {
                final Date dateValue = timeEditor.getValueAsDate();
                return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(), dateValue, false, true);
            }
        }
    }

    @Override
    public void primeEditorValue(@Nullable Object value) {
        timeEditor.setTextValue(valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT));
        try {
            timeEditor.setValue(value);
        } catch (DBCException e) {
            DBWorkbench.getPlatformUI()
                .showWarningMessageBox(
                    ResultSetMessages.dialog_value_view_error_parsing_date_title,
                    ResultSetMessages.dialog_value_view_error_parsing_date_message
                );
        }
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