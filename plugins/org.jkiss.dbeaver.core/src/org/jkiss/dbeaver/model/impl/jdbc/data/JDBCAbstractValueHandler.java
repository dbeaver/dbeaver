/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
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
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);

    public final Object getValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject column, int columnIndex)
        throws DBCException
    {
        try {
            return getColumnValue(context, (JDBCResultSet) resultSet, column, columnIndex + 1);
        }
        catch (Throwable e) {
            throw new DBCException("Could not get result set value", e);
        }
    }

    public final void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnMetaData,
                                      int paramIndex, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCExecutionContext) context, (JDBCPreparedStatement) statement, columnMetaData, paramIndex + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException("Could not bind statement parameter", e);
        }
    }

    public Object createValueObject(DBCExecutionContext context, DBSTypedObject column) throws DBCException
    {
        // Default value for most object types is NULL
        return null;
    }

    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    public String getValueDisplayString(DBSTypedObject column, Object value) {
        return value == null ? DBConstants.NULL_VALUE_LABEL : value.toString();
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException
    {
        return null;
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {

    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "column_size",
            "Column Size",
            controller.getColumnMetaData().getMaxLength());
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
        control.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.closeInlineEditor();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    controller.closeInlineEditor();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
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