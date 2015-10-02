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
package org.jkiss.dbeaver.ui.editors;

import org.jkiss.dbeaver.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * Abstract object command
 */
public class ControlPropertyCommandListener<OBJECT_TYPE extends DBSObject> {

    static final Log log = Log.getLog(ControlPropertyCommandListener.class);

    private final AbstractDatabaseObjectEditor<OBJECT_TYPE> objectEditor;
    private final Widget widget;
    private final DBEPropertyHandler<OBJECT_TYPE> handler;
    private Object originalValue;
    //private Object newValue;
    private DBECommandProperty<OBJECT_TYPE> curCommand;

    public static <OBJECT_TYPE extends DBSObject> void create(
        AbstractDatabaseObjectEditor<OBJECT_TYPE> objectEditor,
        Widget widget,
        DBEPropertyHandler<OBJECT_TYPE> handler)
    {
        new ControlPropertyCommandListener<>(objectEditor, widget, handler);
    }

    public ControlPropertyCommandListener(
        AbstractDatabaseObjectEditor<OBJECT_TYPE> objectEditor,
        Widget widget,
        DBEPropertyHandler<OBJECT_TYPE> handler)
    {
        this.objectEditor = objectEditor;
        this.widget = widget;
        this.handler = handler;

        WidgetListener listener = new WidgetListener();
        widget.addListener(SWT.FocusIn, listener);
        widget.addListener(SWT.FocusOut, listener);
        widget.addListener(SWT.Modify, listener);
        widget.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                //widget.removeListener();
            }
        });
    }

    private Object readWidgetValue()
    {
        if (widget == null || widget.isDisposed()) {
            return null;
        }
        if (widget instanceof Text) {
            return ((Text)widget).getText();
        } else if (widget instanceof Combo) {
            return ((Combo)widget).getText();
        } else if (widget instanceof Button) {
            return ((Button)widget).getSelection();
        } else if (widget instanceof Spinner) {
            return ((Spinner)widget).getSelection();
        } else if (widget instanceof List) {
            return ((List)widget).getSelection();
        } else if (widget instanceof DateTime) {
            DateTime dateTime = (DateTime) widget;
            Calendar cl = Calendar.getInstance();
            cl.set(Calendar.YEAR, dateTime.getYear());
            cl.set(Calendar.MONTH, dateTime.getMonth());
            cl.set(Calendar.DAY_OF_MONTH, dateTime.getDay());
            cl.set(Calendar.HOUR_OF_DAY, dateTime.getHours());
            cl.set(Calendar.MINUTE, dateTime.getMinutes());
            cl.set(Calendar.SECOND, dateTime.getSeconds());
            cl.set(Calendar.MILLISECOND, 0);
            return cl.getTime();
        } else {
            log.warn("Control " + widget + " is not supported");
            return null;
        }
    }

    private void writeWidgetValue(Object value)
    {
        if (widget == null || widget.isDisposed()) {
            return;
        }
        if (widget instanceof Text) {
            ((Text)widget).setText(CommonUtils.toString(value));
        } else if (widget instanceof Combo) {
            ((Combo)widget).setText(CommonUtils.toString(value));
        } else if (widget instanceof Button) {
            ((Button)widget).setSelection(value != null && Boolean.TRUE.equals(value));
        } else if (widget instanceof Spinner) {
            ((Spinner)widget).setSelection(CommonUtils.toInt(value));
        } else if (widget instanceof List) {
            ((List)widget).setSelection((String[])value);
        } else if (widget instanceof DateTime) {
            DateTime dateTime = (DateTime) widget;

            Calendar cl = Calendar.getInstance();
            cl.setTime((Date)value);
            dateTime.setYear(cl.get(Calendar.YEAR));
            dateTime.setMonth(cl.get(Calendar.MONTH));
            dateTime.setDay(cl.get(Calendar.DAY_OF_MONTH));
            dateTime.setHours(cl.get(Calendar.HOUR_OF_DAY));
            dateTime.setMinutes(cl.get(Calendar.MINUTE));
            dateTime.setSeconds(cl.get(Calendar.SECOND));
        } else {
            // not supported
            log.warn("Control " + widget + " is not supported");
        }
    }

    private class WidgetListener implements Listener {
        @Override
        public void handleEvent(Event event)
        {
            switch (event.type) {
                case SWT.FocusIn:
                {
                    originalValue = readWidgetValue();
                    break;
                }
                case SWT.FocusOut:
                {
                    // Forgot current command
                    if (curCommand != null) {
                        curCommand = null;
                    }
                    break;
                }
                case SWT.Modify:
                {
                    final Object newValue = readWidgetValue();
                    DBECommandReflector<OBJECT_TYPE, DBECommandProperty<OBJECT_TYPE>> commandReflector = new DBECommandReflector<OBJECT_TYPE, DBECommandProperty<OBJECT_TYPE>>() {
                        @Override
                        public void redoCommand(DBECommandProperty<OBJECT_TYPE> command)
                        {
                            writeWidgetValue(command.getNewValue());
                        }

                        @Override
                        public void undoCommand(DBECommandProperty<OBJECT_TYPE> command)
                        {
                            writeWidgetValue(command.getOldValue());
                        }
                    };
                    if (curCommand == null) {
                        if (!CommonUtils.equalObjects(newValue, originalValue)) {
                            curCommand = new DBECommandProperty<>(objectEditor.getDatabaseObject(), handler, originalValue, newValue);;
                            objectEditor.addChangeCommand(curCommand, commandReflector);
                        }
                    } else {
                        if (CommonUtils.equalObjects(originalValue, newValue)) {
                            objectEditor.removeChangeCommand(curCommand);
                            curCommand = null;
                        } else {
                            curCommand.setNewValue(newValue);
                            objectEditor.updateChangeCommand(curCommand, commandReflector);
                        }
                    }
                    break;
                }
            }
        }
    }

}