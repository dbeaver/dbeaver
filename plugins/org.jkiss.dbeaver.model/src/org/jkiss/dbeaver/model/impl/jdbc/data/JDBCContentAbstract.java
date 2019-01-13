/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
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

    protected JDBCContentAbstract(DBPDataSource dataSource)
    {
        super(dataSource);
    }

    protected JDBCContentAbstract(JDBCContentAbstract copyFrom) {
        super(copyFrom);
    }

    protected String getDefaultEncoding() {
        return DBValueFormatting.getDefaultBinaryFileEncoding(dataSource);
    }

    public abstract void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

}