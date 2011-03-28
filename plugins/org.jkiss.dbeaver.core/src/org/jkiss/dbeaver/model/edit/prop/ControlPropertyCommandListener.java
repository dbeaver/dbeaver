/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit.prop;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.util.Calendar;
import java.util.Date;

/**
 * Abstract object command
 */
public class ControlPropertyCommandListener<OBJECT_TYPE extends DBSObject> {

    static final Log log = LogFactory.getLog(ControlPropertyCommandListener.class);

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
        new ControlPropertyCommandListener<OBJECT_TYPE>(objectEditor, widget, handler);
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
                    if (curCommand == null) {
                        if (!CommonUtils.equalObjects(newValue, originalValue)) {
                            final DBECommandProperty<OBJECT_TYPE> command = new DBECommandProperty<OBJECT_TYPE>(objectEditor.getDatabaseObject(), handler);
                            command.setOldValue(originalValue);
                            command.setNewValue(objectEditor.getDatabaseObject(), newValue);
                            curCommand = command;
                            DBECommandReflector<OBJECT_TYPE, DBECommandProperty<OBJECT_TYPE>> commandReflector = new DBECommandReflector<OBJECT_TYPE, DBECommandProperty<OBJECT_TYPE>>() {
                                public void redoCommand(DBECommandProperty<OBJECT_TYPE> object_typeControlDBOCommand)
                                {
                                    writeWidgetValue(command.getNewValue());
                                }

                                public void undoCommand(DBECommandProperty<OBJECT_TYPE> object_typeControlDBOCommand)
                                {
                                    writeWidgetValue(command.getOldValue());
                                }
                            };
                            objectEditor.addChangeCommand(curCommand, commandReflector);
                        }
                    } else {
                        if (CommonUtils.equalObjects(originalValue, newValue)) {
                            objectEditor.removeChangeCommand(curCommand);
                            curCommand = null;
                        } else {
                            curCommand.setNewValue(objectEditor.getDatabaseObject(), newValue);
                            objectEditor.updateChangeCommand(curCommand);
                        }
                    }
                    break;
                }
            }
        }
    }

}