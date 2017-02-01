/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                log.debug(e);
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
                preparedStatement.setNull(paramIndex, java.sql.Types.SQLXML);
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
