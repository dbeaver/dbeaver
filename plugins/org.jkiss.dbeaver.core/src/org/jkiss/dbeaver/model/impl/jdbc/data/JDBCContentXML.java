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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.core.Log;
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

    static final Log log = Log.getLog(JDBCContentXML.class);

    private SQLXML xml;
    protected Reader tmpReader;

    public JDBCContentXML(DBPDataSource dataSource, SQLXML xml) {
        super(dataSource);
        this.xml = xml;
    }

    @Override
    public long getLOBLength() throws DBCException {
        return -1;
    }

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
            try {
                xml.free();
            } catch (Exception e) {
                log.warn(e);
            } finally {
                xml = null;
            }
        }
        return storage;
    }

    @Override
    public void release()
    {
        if (tmpReader != null) {
            ContentUtils.close(tmpReader);
            tmpReader = null;
        }
//        if (xml != null) {
//            try {
//                xml.free();
//            } catch (Exception e) {
//                log.warn(e);
//            }
//            xml = null;
//        }
        super.release();
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
