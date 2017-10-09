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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

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
        final int precision = CommonUtils.toInt(valueController.getValueType().getPrecision());
        editor.setTextLimit(precision <= 1 ? 1 : precision);
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
}
