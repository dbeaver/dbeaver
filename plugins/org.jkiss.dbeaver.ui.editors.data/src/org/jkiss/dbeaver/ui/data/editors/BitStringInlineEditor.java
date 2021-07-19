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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* BitStringInlineEditor
*/
public class BitStringInlineEditor extends BaseValueEditor<Text> {
    public BitStringInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Text createControl(Composite editPlaceholder)
    {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        final Text editor = new Text(valueController.getEditPlaceholder(), inline ? SWT.BORDER : SWT.NONE);
        editor.setEditable(!valueController.isReadOnly());
        editor.setTextLimit(getValueLength(valueController.getValueType()));
        editor.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e)
            {
                for (int i = 0; i < e.text.length(); i++) {
                    char ch = e.text.charAt(i);
                    if (ch != '0' && ch != '1') {
                        e.doit = false;
                        return;
                    }
                }
                e.doit = true;
            }
        });
        return editor;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        control.setText(value == null ? "" : value.toString()); //$NON-NLS-1$
    }

    @Override
    public Object extractEditorValue()
    {
        return control.getText();
    }

    private int getValueLength(@NotNull DBSTypedObject object) {
        if (object.getPrecision() != null) {
            return Math.max(1, object.getPrecision());
        } else {
            return Math.max(1, (int) Math.min(object.getMaxLength(), Integer.MAX_VALUE));
        }
    }
}
