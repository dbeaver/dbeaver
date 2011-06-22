/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * JDBCContentXML
 *
 * @author Serge Rider
 */
public class JDBCContentXML extends JDBCContentAbstract implements DBDContent {

    static final Log log = LogFactory.getLog(JDBCContentXML.class);

    private SQLXML xml;
    private DBDContentStorage storage;
    private Reader tmpReader;

    public JDBCContentXML(SQLXML xml) {
        this.xml = xml;
    }

    public long getContentLength() throws DBCException {
        if (storage != null) {
            return storage.getContentLength();
        }
        if (xml == null) {
            return 0;
        }
        return -1;
    }

    public String getContentType()
    {
        return MimeTypes.TEXT_XML;
    }

    public DBDContentStorage getContents(DBRProgressMonitor monitor)
        throws DBCException
    {
        if (storage == null && xml != null) {
            try {
                storage = StringContentStorage.createFromReader(xml.getCharacterStream());
            }
            catch (Exception e) {
                throw new DBCException(e);
            }
        }
        return storage;
    }

    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException
    {
        release();
        this.storage = storage;
        return true;
    }

    public void release()
    {
        if (tmpReader != null) {
            ContentUtils.close(tmpReader);
            tmpReader = null;
        }
        if (storage != null) {
            storage.release();
            storage = null;
        }
        if (xml != null) {
            try {
                xml.free();
            } catch (Exception e) {
                log.warn(e);
            }
            xml = null;
        }
    }

    public void bindParameter(
        DBCExecutionContext context,
        PreparedStatement preparedStatement,
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
            throw new DBCException(e);
        }
        catch (IOException e) {
            throw new DBCException(e);
        }
    }

    public boolean isNull()
    {
        return xml == null && storage == null;
    }

    public JDBCContentXML makeNull()
    {
        return new JDBCContentXML(null);
    }

    @Override
    public String toString() {
        return xml == null && storage == null ? null : "[XML]";
    }

    public DBDValueClonable cloneValue(DBRProgressMonitor monitor)
        throws DBCException
    {
        JDBCContentXML copy = new JDBCContentXML(null);
        DBDContentStorage storage = getContents(monitor);
        try {
            copy.updateContents(monitor, storage.cloneStorage(monitor));
        }
        catch (Exception e) {
            throw new DBCException(e);
        }
        return copy;
    }
}
