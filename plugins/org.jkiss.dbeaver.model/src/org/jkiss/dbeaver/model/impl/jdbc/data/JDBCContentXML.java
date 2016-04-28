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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * JDBCContentXML
 *
 * @author Serge Rider
 */
public class JDBCContentXML extends JDBCContentLOB {

    private static final Log log = Log.getLog(JDBCContentXML.class);

    protected SQLXML xml;

    public JDBCContentXML(DBPDataSource dataSource, SQLXML xml) {
        super(dataSource);
        this.xml = xml;
    }

    @Override
    public long getLOBLength() throws DBCException {
        return -1;
    }

    @NotNull
    @Override
    public String getContentType()
    {
        return MimeTypes.TEXT_XML;
    }

    @Override
    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && xml != null) {
            try {
                storage = StringContentStorage.createFromReader(xml.getCharacterStream());
            }
            catch (IOException e) {
                throw new DBCException("IO error while reading content", e);
            } catch (SQLException e) {
                throw new DBCException(e, dataSource);
            }
            // Free blob - we don't need it anymore
            releaseXML();
        }
        return storage;
    }

    @Override
    public void release()
    {
        releaseXML();
        super.release();
    }

    private void releaseXML() {
        if (xml != null) {
            try {
                xml.free();
            } catch (Exception e) {
                log.warn(e);
            }
            xml = null;
        }
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
                // Try 3 jdbc methods to set character stream
                Reader streamReader = storage.getContentReader();
                try {
                    preparedStatement.setCharacterStream(
                        paramIndex,
                        streamReader);
                }
                catch (Throwable e) {
                    if (e instanceof SQLException) {
                        throw (SQLException)e;
                    } else {
                        long streamLength = ContentUtils.calculateContentLength(storage.getContentReader());
                        try {
                            preparedStatement.setCharacterStream(
                                paramIndex,
                                streamReader,
                                streamLength);
                        }
                        catch (Throwable e1) {
                            if (e1 instanceof SQLException) {
                                throw (SQLException)e1;
                            } else {
                                preparedStatement.setCharacterStream(
                                    paramIndex,
                                    streamReader,
                                    (int)streamLength);
                            }
                        }
                    }
                }
            } else if (xml != null) {
                preparedStatement.setSQLXML(paramIndex, xml);
            } else {
                preparedStatement.setNull(paramIndex + 1, java.sql.Types.SQLXML);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading content", e);
        }
    }

    @Override
    public SQLXML getRawValue() {
        return xml;
    }

    @Override
    public boolean isNull()
    {
        return xml == null && storage == null;
    }

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new JDBCContentXML(dataSource, null);
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        return xml == null && storage == null ? null : "[XML]";
    }

}
