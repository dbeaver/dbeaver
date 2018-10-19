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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RedshiftExternalSchema
 */
public class RedshiftExternalSchema extends PostgreSchema {

    private String esOptions;

    public RedshiftExternalSchema(PostgreDatabase database, String name, String esOptions, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
        this.esOptions = esOptions;
    }

    public RedshiftExternalSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    @Override
    public boolean isExternal() {
        return true;
    }

/*
    protected void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "esoid");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "esowner");
        this.esOptions = JDBCUtils.safeGetString(dbResult, "esoptions");
        this.persisted = true;
    }
*/

    @Property(viewable = true, editable = false, updatable = false, multiline = true, order = 50)
    public String getExternalOptions() {
        return esOptions;
    }

}

