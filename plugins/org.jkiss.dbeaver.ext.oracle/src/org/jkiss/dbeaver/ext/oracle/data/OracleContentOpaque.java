/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * OracleContentOpaque
 *
 * @author Serge Rider
 */
public abstract class OracleContentOpaque<OPAQUE_TYPE extends Object> extends JDBCContentLOB {

    private static final Log log = Log.getLog(OracleContentOpaque.class);

    private OPAQUE_TYPE opaque;
    private InputStream tmpStream;

    public OracleContentOpaque(DBCExecutionContext executionContext, OPAQUE_TYPE opaque) {
        super(executionContext);
        this.opaque = opaque;
    }

    @Override
    public long getLOBLength() throws DBCException {
        return 0;//opaque.getLength();
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
        if (storage == null && opaque != null) {
            storage = makeStorageFromOpaque(monitor, opaque);
            opaque = null;
        }
        return storage;
    }

    @Override
    public void release()
    {
        if (tmpStream != null) {
            ContentUtils.close(tmpStream);
            tmpStream = null;
        }
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
                preparedStatement.setObject(paramIndex, createNewOracleObject(session.getOriginal()));
            } else if (opaque != null) {
                preparedStatement.setObject(paramIndex, opaque);
            } else {
                preparedStatement.setNull(paramIndex, java.sql.Types.SQLXML);
            }
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading content", e);
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public boolean isNull()
    {
        return opaque == null && storage == null;
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format)
    {
        return opaque == null && storage == null ? null : "[" + getOpaqueType() + "]";
    }

    protected abstract String getOpaqueType();

    @Override
    protected abstract OracleContentOpaque createNewContent();

    protected abstract OPAQUE_TYPE createNewOracleObject(Connection connection)
        throws DBCException, IOException, SQLException;

    protected abstract DBDContentStorage makeStorageFromOpaque(DBRProgressMonitor monitor, OPAQUE_TYPE opaque) throws DBCException;

}
