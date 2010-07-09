/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.streams.MimeTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public class JDBCContentCLOB extends JDBCContentAbstract implements DBDContentCharacter {

    static Log log = LogFactory.getLog(JDBCContentCLOB.class);

    private Clob clob;
    private DBDContentStorage storage;
    private Reader tmpReader;

    public JDBCContentCLOB(Clob clob) {
        this.clob = clob;
    }

    public long getContentLength() throws DBCException {
        if (clob == null) {
            return 0;
        }
        try {
            return clob.length();
        } catch (SQLException e) {
            throw new DBCException("JDBC error", e);
        }
    }

    public String getContentType()
    {
        return MimeTypes.TEXT_PLAIN;
    }

    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDValueController valueController,
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
    }

    public String getCharset()
    {
        if (storage != null) {
            return storage.getCharset();
        }
        return null;
    }

    public Reader getContents() throws DBCException {
        if (storage != null) {
            try {
                return new InputStreamReader(storage.getContentStream(), storage.getCharset());
            }
            catch (IOException e) {
                throw new DBCException(e);
            }
        }
        if (clob != null) {
            try {
                return clob.getCharacterStream();
            } catch (SQLException e) {
                throw new DBCException("JDBC error", e);
            }
        }
        return new StringReader("");
    }

    public void bindParameter(DBRProgressMonitor monitor, PreparedStatement preparedStatement,
                              DBSTypedObject columnType, int paramIndex)
        throws DBCException
    {
        try {
            if (storage != null) {
                // Try 3 jdbc methods to set character stream
                InputStreamReader streamReader = new InputStreamReader(storage.getContentStream(), storage.getCharset());
                try {
                    preparedStatement.setCharacterStream(
                        paramIndex,
                        streamReader);
                }
                catch (AbstractMethodError e) {
                    long streamLength = ContentUtils.calculateContentLength(storage.getContentStream(), storage.getCharset());
                    try {
                        preparedStatement.setCharacterStream(
                            paramIndex,
                            streamReader,
                            streamLength);
                    }
                    catch (AbstractMethodError e1) {
                        preparedStatement.setCharacterStream(
                            paramIndex,
                            streamReader,
                            (int)streamLength);
                    }
                }
            } else if (clob != null) {
                preparedStatement.setClob(paramIndex, clob);
            } else {
                preparedStatement.setNull(paramIndex + 1, java.sql.Types.CLOB);
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
        return clob == null && storage == null;
    }

    public JDBCContentCLOB makeNull()
    {
        return new JDBCContentCLOB(null);
    }

    @Override
    public String toString() {
        return clob == null && storage == null ? null : "[CLOB]";
    }

    public DBDValueClonable cloneValue()
    {
        return null;
    }
}
