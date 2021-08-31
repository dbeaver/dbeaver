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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * PostgreDataTypeAttribute
 */
public class PostgreDataTypeAttribute extends PostgreAttribute<PostgreDataType>
{
    public PostgreDataTypeAttribute(DBRProgressMonitor monitor, PostgreDataType dataType, JDBCResultSet dbResult) throws DBException {
        super(monitor, dataType, dbResult);
    }

    @Override
    public int getOrdinalPosition() {
        return super.getOrdinalPosition() - 1;
    }

    @NotNull
    @Override
    public PostgreSchema getSchema() {
        return getDataType().getParentObject();
    }
}
