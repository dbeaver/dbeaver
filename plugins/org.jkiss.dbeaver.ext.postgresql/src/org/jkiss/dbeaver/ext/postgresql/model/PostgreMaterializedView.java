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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * PostgreMaterializedView
 */
public class PostgreMaterializedView extends PostgreViewBase
{
    public PostgreMaterializedView(PostgreSchema catalog) {
        super(catalog);
    }

    public PostgreMaterializedView(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return getSchema().indexCache.getObjects(monitor, getSchema(), this);
    }

    public String getViewType() {
        return "MATERIALIZED VIEW";
    }

}
