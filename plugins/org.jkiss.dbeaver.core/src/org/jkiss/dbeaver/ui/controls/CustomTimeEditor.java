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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
    private Text timeEditor;
//    private Button timePickerButton;

    public CustomTimeEditor(Composite parent, int style) {
        this.timeEditor = new Text(parent, style);
/*
        Composite ph = UIUtils.createPlaceholder(parent, 2);
        this.timeEditor = new Text(ph, style);

        this.timePickerButton = new Button(ph, SWT.FLAT | SWT.ARROW | SWT.DOWN);
        this.timePickerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                UIUtils.showMessageBox(timePickerButton.getShell(), "asdf", "sdf", SWT.ICON_INFORMATION);
            }
        });
*/
    }

    public void setValue(@Nullable String value) {
        if (value == null) {
            timeEditor.setText("");
        } else {
            timeEditor.setText(value);
        }
    }

    public String getValue()
        throws DBException {
        final String timeText = timeEditor.getText();
        if (timeText.isEmpty()) {
            return null;
        }
        return timeText;
    }


    public void setEnabled(boolean enabled) {
        timeEditor.setEnabled(enabled);
    }

    public Text getControl() {
        return timeEditor;
    }

    public void selectAll() {
        timeEditor.selectAll();
    }
}
