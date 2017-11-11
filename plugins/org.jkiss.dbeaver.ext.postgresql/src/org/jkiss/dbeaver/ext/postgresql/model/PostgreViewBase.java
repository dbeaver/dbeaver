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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * PostgreViewBase
 */
public abstract class PostgreViewBase extends PostgreTableReal
{
    private String source;

    public PostgreViewBase(PostgreSchema catalog)
    {
        super(catalog);
    }

    public PostgreViewBase(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public String getSource() {
        return source;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        if (source == null) {
            if (isPersisted()) {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read view definition")) {
                    String definition = JDBCUtils.queryString(session, "SELECT pg_get_viewdef(?, true)", getObjectId());
                    this.source = PostgreUtils.getViewDDL(this, definition);
                    String extDefinition = readExtraDefinition(session, options);
                    if (extDefinition != null) {
                        this.source += "\n" + extDefinition;
                    }
                } catch (SQLException e) {
                    throw new DBException("Error reading view definition", e);
                }
            } else {
                source = "";
            }
        }
        return source;
    }

    protected String readExtraDefinition(JDBCSession session, Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        this.source = sourceText;
    }

    public abstract String getViewType();

}
