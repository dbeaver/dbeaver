/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.DBDStreamHandler;
import org.jkiss.dbeaver.model.data.DBDStreamKind;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCContentBinary;
import org.jkiss.dbeaver.model.dbc.DBCContentCharacter;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCContent;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler implements DBDStreamHandler {

    static Log log = LogFactory.getLog(JDBCContentValueHandler.class);

    public static final JDBCContentValueHandler INSTANCE = new JDBCContentValueHandler();

    protected Object getValueObject(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        return resultSet.getObject(columnIndex);
    }

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
    }

    public String getValueDisplayString(Object value)
    {
        if (value instanceof byte[]) {
            return "binary [" + ((byte[]) value).length + "]";
        } else if (value instanceof DBCContentBinary || value instanceof Blob) {
            return "BLOB";
        } else if (value instanceof DBCContentCharacter || value instanceof Clob) {
            return "CLOB";
        } else {
            return super.getValueDisplayString(value);
        }
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
            propertySource.addProperty(
                "content_type",
                "Content Type",
                getContentType(controller.getValue()));
            propertySource.addProperty(
                "content_length",
                "Content Length",
                getContentLength(controller.getValue()));
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

    public DBDStreamKind getContentKind(DBCColumnMetaData columnMetaData)
    {
        switch (columnMetaData.getValueType()) {
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
            case java.sql.Types.CLOB:
            case java.sql.Types.NCLOB:
                return DBDStreamKind.CHARACTER;
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
            case java.sql.Types.BLOB:
            default:
                return DBDStreamKind.BINARY;
        }
    }

    public Object getContents(Object value)
        throws DBCException, IOException
    {
        try {
            if (value == null) {
                return null;
            } else if (value instanceof byte[]) {
                return new ByteArrayInputStream((byte[]) value);
            } else if (value instanceof String) {
                return new StringReader((String)value);
            } else if (value instanceof DBCContentBinary) {
                return ((DBCContentBinary)value).getContents();
            } else if (value instanceof DBCContentCharacter) {
                return ((DBCContentCharacter)value).getContents();
            } else if (value instanceof Blob) {
                return ((Blob)value).getBinaryStream();
            } else if (value instanceof Clob) {
                return ((Clob)value).getCharacterStream();
            } else {
                throw new DBCException("Unsupported value type: " + value.getClass().getName());
            }
        }
        catch (SQLException e) {
            throw new DBCException("Could not extract stream from value", e);
        }
    }

    public long getContentLength(Object value)
        throws DBCException, IOException
    {
        try {
            if (value == null) {
                return 0;
            } else if (value instanceof byte[]) {
                return ((byte[]) value).length;
            } else if (value instanceof String) {
                return ((String)value).length();
            } else if (value instanceof DBCContent) {
                return ((DBCContent)value).getContentLength();
            } else if (value instanceof Blob) {
                return ((Blob)value).length();
            } else if (value instanceof Clob) {
                return ((Clob)value).length();
            } else {
                throw new DBCException("Unsupported value type: " + value.getClass().getName());
            }
        }
        catch (SQLException e) {
            throw new DBCException("Could not extract stream length from value", e);
        }
    }

    public String getContentType(Object value)
        throws DBCException, IOException
    {
        if (value instanceof byte[] || value instanceof DBCContentBinary || value instanceof Blob) {
            return MimeTypes.OCTET_STREAM;
        } else if (value instanceof String || value instanceof DBCContentCharacter || value instanceof Clob) {
            return MimeTypes.TEXT_PLAIN;
        } else {
            return null;
        }
    }

    public String getContentEncoding(Object value)
        throws DBCException, IOException
    {
        return null;
    }

    public Object updateContents(Object value, Object content, long contentSize)
        throws DBCException, IOException
    {
        return null;
    }

}
