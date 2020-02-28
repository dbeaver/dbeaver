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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

/**
* URLPreviewEditor
*/
public class URLPreviewEditor extends BaseValueEditor<Browser> {

    private String lastURL;

    public URLPreviewEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Browser createControl(Composite editPlaceholder)
    {
        Browser browser = new Browser(editPlaceholder, SWT.NONE);
        // Set browser settings

        return browser;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        if (control != null) {
            final String strValue = valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT);
            if (CommonUtils.equalObjects(lastURL, strValue)) {
                return;
            }
            lastURL = strValue;
            control.setUrl("about:blank");
            control.setText("<div>Loading " + strValue + "...</div>");
            control.setUrl(strValue);
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        return control == null? null : control.getUrl();
    }
}
