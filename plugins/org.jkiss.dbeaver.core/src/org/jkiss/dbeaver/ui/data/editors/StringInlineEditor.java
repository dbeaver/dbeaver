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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* StringInlineEditor.
 * Relies on StyledText. After all it is better.
*/
public class StringInlineEditor extends BaseValueEditor<Control> {

    private static final int MAX_STRING_LENGTH = 0xffff;

    public StringInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        if (inline) {
            final Text editor = new Text(valueController.getEditPlaceholder(), SWT.BORDER);
            editor.setTextLimit(MAX_STRING_LENGTH);
            editor.setEditable(!valueController.isReadOnly());
            return editor;
        } else {
            final StyledText editor = new StyledText(valueController.getEditPlaceholder(),
                SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            editor.setTextLimit(MAX_STRING_LENGTH);
            editor.setEditable(!valueController.isReadOnly());
            editor.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
            StyledTextUtils.fillDefaultStyledTextContextMenu(editor);
            return editor;
        }
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        final String strValue = valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT);
        if (control instanceof Text) {
            ((Text)control).setText(strValue);
            if (valueController.getEditType() == IValueController.EditType.INLINE) {
                ((Text)control).selectAll();
            }
        } else {
            ((StyledText)control).setText(strValue);
            if (valueController.getEditType() == IValueController.EditType.INLINE) {
                ((StyledText)control).selectAll();
            }
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        try (DBCSession session = valueController.getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make string value from editor")) {
            String text;
            if (control instanceof Text) {
                text = ((Text) control).getText();
            } else {
                text = ((StyledText) control).getText();
            }
            return valueController.getValueHandler().getValueFromObject(
                session,
                valueController.getValueType(),
                text,
                false);
        }
    }
}
