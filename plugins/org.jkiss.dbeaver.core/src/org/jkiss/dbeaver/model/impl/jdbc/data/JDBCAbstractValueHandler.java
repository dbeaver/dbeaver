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
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);
    private static final String CELL_VALUE_INLINE_EDITOR = "org.jkiss.dbeaver.CellValueInlineEditor";

    @Override
    public final Object fetchValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException
    {
        try {
            return fetchColumnValue(context, (JDBCResultSet) resultSet, type, index + 1);
        }
        catch (Throwable e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_get_result_set_value, e);
        }
    }

    @Override
    public final void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnMetaData,
                                      int index, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCExecutionContext) context, (JDBCPreparedStatement) statement, columnMetaData, index + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Override
    public Object getValueFromClipboard(DBCExecutionContext context, DBSTypedObject column, Clipboard clipboard) throws DBCException
    {
        String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
        if (CommonUtils.isEmpty(strValue)) {
            return null;
        }
        return getValueFromObject(context, column, strValue, false);
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

    protected static interface ValueExtractor <T extends Control> {
         Object getValueFromControl(T control);
    }

    protected <T extends Control> void initInlineControl(
        final DBDValueController controller,
        final T control,
        final ValueExtractor<T> extractor)
    {
        UIUtils.addFocusTracker(controller.getValueSite(), CELL_VALUE_INLINE_EDITOR, control);
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                UIUtils.removeFocusTracker(controller.getValueSite(), control);
            }
        });

        if (controller.getEditType() == DBDValueController.EditType.PANEL) {
            control.setBackground(controller.getEditPlaceholder().getBackground());
            return;
        }
        // There is a bug in windows. First time date control gain focus it renders cell editor incorrectly.
        // Let's focus on it in async mode
        control.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                control.setFocus();
            }
        });

        control.setFont(controller.getEditPlaceholder().getFont());
        control.addTraverseListener(new TraverseListener() {
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
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                // Check new focus control in async mode
                // (because right now focus is still on edit control)
                control.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        if (control.isDisposed()) {
                            return;
                        }
                        Control newFocus = control.getDisplay().getFocusControl();
                        if (newFocus != null) {
                            for (Control fc = newFocus.getParent(); fc != null; fc = fc.getParent()) {
                                if (fc == controller.getEditPlaceholder()) {
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
    }

    protected abstract Object fetchColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException, SQLException;

    protected abstract void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

}