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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.AbstractContent;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * JDBCContentAbstract
 *
 * @author Serge Rider
 */
public abstract class JDBCContentAbstract extends AbstractContent implements DBDValueCloneable {

    protected JDBCContentAbstract(DBCExecutionContext executionContext)
    {
        super(executionContext);
    }

    protected JDBCContentAbstract(JDBCContentAbstract copyFrom) {
        super(copyFrom);
    }

    protected String getDefaultEncoding() {
        return DBValueFormatting.getDefaultBinaryFileEncoding(executionContext.getDataSource());
    }

    public abstract void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

}