/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

/**
* URLPreviewEditor
*/
public class URLPreviewEditor extends BaseValueEditor<Browser> {

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
            control.setText("<div>Loading " + strValue + "...</div>");
            control.setUrl(strValue);
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        return control == null? null : control.getUrl();
    }
}
