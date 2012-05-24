/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
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

    static final Log log = LogFactory.getLog(JDBCContentXML.class);

    private SQLXML xml;
    protected Reader tmpReader;

    public JDBCContentXML(SQLXML xml) {
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
            catch (Exception e) {
                throw new DBCException(e);
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
        JDBCExecutionContext context,
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
            throw new DBCException(e);
        }
        catch (IOException e) {
            throw new DBCException(e);
        }
    }

    @Override
    public boolean isNull()
    {
        return xml == null && storage == null;
    }

    @Override
    protected JDBCContentLOB createNewContent()
    {
        return new JDBCContentXML(null);
    }

    @Override
    public String toString() {
        return xml == null && storage == null ? null : "[XML]";
    }

}
