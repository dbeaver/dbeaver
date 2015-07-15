/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.data.DBDDataFormatter;

import java.util.Date;

/**
* CustomTimeEditor
*/
public class CustomTimeEditor {
    private DBDDataFormatter formatter;
    private Text timeEditor;

    public CustomTimeEditor(Composite parent, int style, DBDDataFormatter formatter) {
        this.timeEditor = new Text(parent, style);
        this.formatter = formatter;
    }

    public void setValue(@Nullable Object value)
    {
        if (value == null) {
            value = new Date(0l);
        }
        String timeText;
        if (value instanceof Date) {
            timeText = formatter.formatValue(value);
        } else if (value instanceof String) {
            timeText = (String) value;
        } else {
            timeText = value.toString();
        }
        timeEditor.setText(timeText);
    }

    public Date getValue()
        throws DBException
    {
        final String timeText = timeEditor.getText();
        if (timeText.isEmpty()) {
            return null;
        }
        try {
            return (Date)formatter.parseValue(timeText, Date.class);
        } catch (final Exception e) {
            throw new DBException("Error parsing date value [" + timeText + "]", e);
        }
    }


    public void setEnabled(boolean enabled) {
        timeEditor.setEnabled(enabled);
    }

    public Control getControl() {
        return timeEditor;
    }
}
