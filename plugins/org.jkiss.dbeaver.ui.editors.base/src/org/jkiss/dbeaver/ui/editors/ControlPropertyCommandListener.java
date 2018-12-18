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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
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

    private static final Log log = Log.getLog(ControlPropertyCommandListener.class);

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