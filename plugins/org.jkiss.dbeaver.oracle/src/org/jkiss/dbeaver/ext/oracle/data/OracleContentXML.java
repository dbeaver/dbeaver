/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentXML;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML content
 */
public class OracleContentXML extends JDBCContentXML {
    public OracleContentXML(SQLXML xml)
    {
        super(xml);
    }

    @Override
    protected OracleContentXML createNewContent()
    {
        return new OracleContentXML(null);
    }

    public void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement preparedStatement,
        DBSTypedObject columnType,
        int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                InputStream streamReader = storage.getContentStream();
                try {
                    final Object xmlObject = createXmlObject(context, streamReader);

                    preparedStatement.setObject(
                        paramIndex,
                        xmlObject);
                } finally {
                    ContentUtils.close(streamReader);
                }
            } else {
                preparedStatement.setNull(paramIndex + 1, java.sql.Types.SQLXML);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
        catch (IOException e) {
            throw new DBCException(e);
        }
    }

    private Object createXmlObject(JDBCExecutionContext context, InputStream stream) throws DBCException
    {
        try {
            final Class<?> xmlTypeClass = context.getOriginal().getClass().getClassLoader().loadClass("oracle.xdb.XMLType");
            return xmlTypeClass.getMethod("createXML", Connection.class, InputStream.class).invoke(null, context.getOriginal(), stream);
        } catch (Exception e) {
            throw new DBCException(e);
        }
    }

/*
    @Override
    protected XMLType createNewOracleObject(Connection connection) throws DBCException, IOException, SQLException
    {
        final InputStream contentStream = storage.getContentStream();
        try {
            return XMLType.createXML(connection, contentStream);
        } finally {
            ContentUtils.close(contentStream);
        }
    }

    @Override
    protected DBDContentStorage makeStorageFromOpaque(DBRProgressMonitor monitor, XMLType opaque) throws DBCException
    {
        long contentLength = opaque.getLength();
        if (contentLength < 4000) {
            try {
                return new StringContentStorage(opaque.getStringVal());
            } catch (SQLException e) {
                throw new DBCException(e);
            }
        } else {
            // Create new local storage
            IFile tempFile;
            try {
                tempFile = ContentUtils.createTempContentFile(monitor, "opaque" + opaque.hashCode());
            }
            catch (IOException e) {
                throw new DBCException(e);
            }
            try {
                ContentUtils.copyReaderToFile(monitor, opaque.getClobVal().getCharacterStream(), contentLength, null, tempFile);
            } catch (Exception e) {
                ContentUtils.deleteTempFile(monitor, tempFile);
                throw new DBCException(e);
            }
            return new TemporaryContentStorage(tempFile);
        }
    }
*/
}
