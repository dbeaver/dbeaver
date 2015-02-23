/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ext.oracle.data;

import oracle.xdb.XMLType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentXML;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

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
            return XMLType.createXML(session.getOriginal(), stream);
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
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
