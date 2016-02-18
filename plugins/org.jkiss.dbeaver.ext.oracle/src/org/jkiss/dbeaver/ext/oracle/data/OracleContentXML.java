/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentXML;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML content
 */
public class OracleContentXML extends JDBCContentXML {
    public OracleContentXML(DBPDataSource dataSource, SQLXML xml)
    {
        super(dataSource, xml);
    }

    @Override
    protected OracleContentXML createNewContent()
    {
        return new OracleContentXML(dataSource, null);
    }

    @Override
    public void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement preparedStatement,
        DBSTypedObject columnType,
        int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                InputStream streamReader = storage.getContentStream();
                try {
                    final Object xmlObject = createXmlObject(session, streamReader);

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
            throw new DBCException(e, session.getDataSource());
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading XML", e);
        }
    }

    private Object createXmlObject(JDBCSession session, InputStream stream) throws DBCException
    {
        try {
            return BeanUtils.invokeStaticMethod(
                DBUtils.getDriverClass(dataSource, OracleConstants.XMLTYPE_CLASS_NAME),
                "createXML",
                new Class[] {java.sql.Connection.class, java.io.InputStream.class},
                new Object[] {session.getOriginal(), stream});
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        } catch (Throwable e) {
            throw new DBCException("Internal error when creating XMLType", e, session.getDataSource());
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
