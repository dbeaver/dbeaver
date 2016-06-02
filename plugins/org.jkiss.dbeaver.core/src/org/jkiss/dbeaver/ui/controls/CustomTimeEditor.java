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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
* CustomTimeEditor
*/
public class CustomTimeEditor {
    private Text timeEditor;

    public CustomTimeEditor(Composite parent, int style) {
        this.timeEditor = new Text(parent, style);
    }

    public void setValue(@Nullable String value)
    {
        if (value == null) {
            timeEditor.setText("");
        } else {
            timeEditor.setText(value);
        }
    }

    public String getValue()
        throws DBException
    {
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
}
