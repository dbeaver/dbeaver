/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommandReflector;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.util.Calendar;
import java.util.Date;

/**
 * Abstract object command
 */
public abstract class ControlCommandListener {

    static final Log log = LogFactory.getLog(ControlCommandListener.class);

    private final AbstractDatabaseObjectEditor objectEditor;
    private final Widget widget;
    private Object originalValue;
    //private Object newValue;

    protected ControlCommandListener(AbstractDatabaseObjectEditor objectEditor, Widget widget)
    {
        this.objectEditor = objectEditor;
        this.widget = widget;

        WidgetListener listener = new WidgetListener();
        widget.addListener(SWT.FocusIn, listener);
        widget.addListener(SWT.FocusOut, listener);
        widget.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                //widget.removeListener();
            }
        });
    }

    protected abstract ControlDatabaseObjectCommand createCommand();

    public Widget getWidget()
    {
        return widget;
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
            return ((Spinner)widget).getText();
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
            ((Text)widget).setText(value == null ? "" : value.toString());
        } else if (widget instanceof Combo) {
            ((Combo)widget).setText(value == null ? "" : value.toString());
        } else if (widget instanceof Button) {
            ((Button)widget).setSelection(value != null && Boolean.TRUE.equals(value));
        } else if (widget instanceof Spinner) {
            try {
                ((Spinner)widget).setSelection(value == null ? 0 : Integer.parseInt(value.toString()));
            } catch (NumberFormatException e) {
                log.debug(e);
            }
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
                    final Object newValue = readWidgetValue();
                    if (!CommonUtils.equalObjects(newValue, originalValue)) {
                        final ControlDatabaseObjectCommand command = createCommand();
                        command.setOldValue(originalValue);
                        command.setNewValue(newValue);
                        objectEditor.addChangeCommand(command, new IDatabaseObjectCommandReflector() {
                            public void redoCommand(IDatabaseObjectCommand iDatabaseObjectCommand)
                            {
                                writeWidgetValue(command.getNewValue());
                            }

                            public void undoCommand(IDatabaseObjectCommand iDatabaseObjectCommand)
                            {
                                writeWidgetValue(command.getOldValue());
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

}