/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);
    private static final String CELL_VALUE_INLINE_EDITOR = "org.jkiss.dbeaver.CellValueInlineEditor";

    @Override
    public final Object getValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException
    {
        try {
            return getColumnValue(context, (JDBCResultSet) resultSet, column, columnIndex + 1);
        }
        catch (Throwable e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_get_result_set_value, e);
        }
    }

    @Override
    public final void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnMetaData,
                                      int paramIndex, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCExecutionContext) context, (JDBCPreparedStatement) statement, columnMetaData, paramIndex + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Override
    public Object createValueObject(DBCExecutionContext context, DBSTypedObject column) throws DBCException
    {
        // Default value for most object types is NULL
        return null;
    }

    @Override
    public Object getValueFromClipboard(DBSTypedObject column, Clipboard clipboard)
    {
        // By default handler doesn't support any clipboard format
        return null;
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value) {
        return value == null ? DBConstants.NULL_VALUE_LABEL : value.toString();
    }

    @Override
    public DBDValueAnnotation[] getValueAnnotations(DBCAttributeMetaData column)
        throws DBCException
    {
        return null;
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {

    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "column_size", //$NON-NLS-1$
            CoreMessages.model_jdbc_column_size,
            controller.getAttributeMetaData().getMaxLength());
    }

    @Override
    public DBDValueViewer createValueViewer(final DBDValueController controller) throws DBException
    {
        final Text text = new Text(controller.getInlinePlaceholder(), SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        return new DBDValueViewer() {
            @Override
            public void showValue(DBDValueController controller1)
            {
                text.setText(getValueDisplayString(controller1.getAttributeMetaData(), controller1.getValue()));
            }
        };
    }

    protected static interface ValueExtractor <T extends Control> {
         Object getValueFromControl(T control);
    }

    protected <T extends Control> void initInlineControl(
        final DBDValueController controller,
        final T control,
        final ValueExtractor<T> extractor)
    {
        control.setFont(controller.getInlinePlaceholder().getFont());
        control.addTraverseListener(new TraverseListener()
        {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    Object newValue = extractor.getValueFromControl(control);
                    controller.closeInlineEditor();
                    controller.updateValue(newValue);
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    controller.closeInlineEditor();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    Object newValue = extractor.getValueFromControl(control);
                    controller.closeInlineEditor();
                    controller.updateValue(newValue);
                    controller.nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });
        UIUtils.addFocusTracker(controller.getValueSite(), CELL_VALUE_INLINE_EDITOR, control);
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                // Check new focus control in async mode
                // (because right now focus is still on edit control)
                control.getDisplay().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (control.isDisposed()) {
                            return;
                        }
                        Control newFocus = control.getDisplay().getFocusControl();
                        if (newFocus != null) {
                            for (Control fc = newFocus.getParent(); fc != null; fc = fc.getParent()) {
                                if (fc == controller.getInlinePlaceholder()) {
                                    // New focus is still a child of inline placeholder - do not close it
                                    return;
                                }
                            }
                        }
                        controller.updateValue(extractor.getValueFromControl(control));
                        controller.closeInlineEditor();
                    }
                });
            }
        });
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.removeFocusTracker(controller.getValueSite(), control);
            }
        });
    }

    protected abstract Object getColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException, SQLException;

    protected abstract void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

}