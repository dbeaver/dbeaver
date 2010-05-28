/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler {

    static Log log = LogFactory.getLog(JDBCContentValueHandler.class);

    public static final JDBCContentValueHandler INSTANCE = new JDBCContentValueHandler();

    protected DBDContent getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            // Create wrapper using column type
            switch (columnType.getValueType()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                    return new JDBCContentChars(null);
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return new JDBCContentCLOB(null);
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(null);
                case java.sql.Types.BLOB:
                    return new JDBCContentBLOB(null);
                default:
                    throw new DBCException("Unsupported column type: " + columnType.getTypeName());
            }
        } else if (value instanceof byte[]) {
            return new JDBCContentBytes((byte[]) value);
        } else if (value instanceof String) {
            return new JDBCContentChars((String) value);
        } else if (value instanceof Blob) {
            return new JDBCContentBLOB((Blob) value);
        } else if (value instanceof Clob) {
            return new JDBCContentCLOB((Clob) value);
        } else {
            throw new DBCException("Unsupported value type: " + value.getClass().getName());
        }
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(statement, paramType, paramIndex);
        } else {
            throw new DBCException("Unsupported value type: " + value);
        }
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).release();
        }
        super.releaseValueObject(value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getValueDisplayString(Object value)
    {
        if (value instanceof DBDContent) {
            String result = value.toString();
            if (result != null) {
                return result;
            } else {
                return super.getValueDisplayString(null);
            }
        }
        return super.getValueDisplayString(value);
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {
        Action saveAction = new Action() {
            @Override
            public void run() {
            }
        };
        saveAction.setText("Save to file ...");
        menuManager.add(saveAction);
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                    "content_type",
                    "Content Type",
                    ((DBDContent)value).getContentType());
                propertySource.addProperty(
                    "content_length",
                    "Content Length",
                    ((DBDContent)value).getContentLength());
            }
        }
        catch (Exception e) {
            log.warn("Could not extract LOB value information", e);
        }
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getColumnMetaData().getDisplaySize());
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            // Open inline editor
            return false;
        }
        // Open LOB editor
        return ContentEditor.openEditor(
            controller,
            new IContentEditorPart[] {
                new ContentBinaryEditorPart(),
                new ContentTextEditorPart(),
                new ContentImageEditorPart()} );
    }

}
