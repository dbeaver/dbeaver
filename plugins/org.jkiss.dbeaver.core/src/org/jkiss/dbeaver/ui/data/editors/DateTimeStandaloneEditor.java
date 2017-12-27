/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.impl.data.DateTimeCustomValueHandler;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.data.ValueViewDialog;

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
    protected Control createDialogArea(Composite parent)
    {
        IValueController valueController = getValueController();
        Object value = valueController.getValue();

        Composite dialogGroup = (Composite)super.createDialogArea(parent);
        Composite panel = UIUtils.createPlaceholder(dialogGroup, 3);
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        int style = SWT.BORDER;
        if (valueController.isReadOnly()) {
            style |= SWT.READ_ONLY;
        }
        String formaterId = "";
		Object valueHandler = valueController.getValueHandler();
		if (valueHandler instanceof DateTimeCustomValueHandler) {
			DateTimeCustomValueHandler dateTimeValueHandler = (DateTimeCustomValueHandler) valueHandler;
			formaterId = dateTimeValueHandler.getFormatterId(valueController.getValueType());
		}
        UIUtils.createControlLabel(panel, "Time").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        timeEditor = new CustomTimeEditor(panel, style,formaterId);
		timeEditor.addSelectionAdapter(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dirty = true;
			}
		});
 
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalAlignment = GridData.CENTER;
        timeEditor.getControl().setLayoutData(gd);

        primeEditorValue(value);

        Button button = UIUtils.createPushButton(panel, "Set Current", null);
        button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        button.setEnabled(!valueController.isReadOnly());
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                primeEditorValue(new Date());
            }
        });

        return dialogGroup;
    }

    @Override
    public Object extractEditorValue() throws DBException {
        return getValueController().getValueHandler().getValueFromObject(null, getValueController().getValueType(), timeEditor.getValue(), false);
    }

    @Override
    public void primeEditorValue(@Nullable Object value)
    {
        if (value instanceof Date) {
            timeEditor.setValue((Date) value);
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