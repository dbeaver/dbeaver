/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDStreamHandler;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCBLOB;
import org.jkiss.dbeaver.model.dbc.DBCCLOB;
import org.jkiss.dbeaver.model.dbc.DBCLOB;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.lob.LOBEditor;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Blob;
import java.sql.Clob;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.Reader;

import net.sf.jkiss.utils.streams.MimeTypes;

/**
 * JDBC LOB value handler.
 * Handle LOBs, LONGs and BINARY types.
 */
public class JDBCLOBValueHandler extends JDBCAbstractValueHandler implements DBDStreamHandler {

    static Log log = LogFactory.getLog(JDBCLOBValueHandler.class);

    public static final JDBCLOBValueHandler INSTANCE = new JDBCLOBValueHandler();

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
        } else if (value instanceof DBCBLOB || value instanceof Blob) {
            return "BLOB";
        } else if (value instanceof DBCCLOB || value instanceof Clob) {
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
        } else {
            // Open LOB editor
            return LOBEditor.openEditor(controller);
        }
    }

    public InputStream getContentStream(Object value)
        throws DBCException, IOException
    {
        try {
            if (value == null) {
                return null;
            } else if (value instanceof byte[]) {
                return new ByteArrayInputStream((byte[]) value);
            } else if (value instanceof String) {
                return makeStreamFromString((String)value);
            } else if (value instanceof DBCBLOB) {
                return ((DBCBLOB)value).getBinaryStream();
            } else if (value instanceof DBCCLOB) {
                return makeStreamFromString(
                    readStringFromReader(((DBCCLOB)value).getCharacterStream()));
            } else if (value instanceof Blob) {
                return ((Blob)value).getBinaryStream();
            } else if (value instanceof Clob) {
                return makeStreamFromString(
                    readStringFromReader(((Clob)value).getCharacterStream()));
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
            } else if (value instanceof DBCLOB) {
                return ((DBCLOB)value).getLength();
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
        if (value instanceof byte[] || value instanceof DBCBLOB || value instanceof Blob) {
            return MimeTypes.OCTET_STREAM;
        } else if (value instanceof String || value instanceof DBCCLOB || value instanceof Clob) {
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

    public Object updateContent(Object value, InputStream content, long contentSize)
        throws DBCException, IOException
    {
        return null;
    }

    private InputStream makeStreamFromString(String string)
    {
        return new ByteArrayInputStream(string.getBytes());
    }

    private String readStringFromReader(Reader in)
        throws IOException
    {
        StringBuilder result = new StringBuilder();
        char[] buf = new char[10000];
        for (;;) {
            int len = in.read(buf);
            if (len <= 0) {
                break;
            }
            result.append(buf, 0, len);
        }
        return result.toString();
    }

}
