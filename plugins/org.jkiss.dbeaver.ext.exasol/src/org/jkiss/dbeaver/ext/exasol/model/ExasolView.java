/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;

public class ExasolView extends ExasolTableBase implements ExasolSourceObject {


    private String owner;
    private String description;

    private String text;

    public ExasolView(ExasolSchema schema, String name, boolean persisted) {
        super(schema, name, persisted);
    }

    public ExasolView(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        this.text = JDBCUtils.safeGetString(dbResult, "VIEW_TEXT");

    }


    @Override
    public DBSObjectState getObjectState() {
        return DBSObjectState.NORMAL;
    }


    @Override
    @Property(viewable = true, editable = false, order = 40)
    public String getDescription() {
        return this.description;
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Property(viewable = true, order = 100)
    public String getOwner() {
        return owner;
    }

    @Override
    public boolean isView() {
        return true;
    }


    // -----------------
    // Business Contract
    // -----------------

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        return this;
    }


    // -----------------
    // Associations (Imposed from DBSTable). In Exasol, Most of objects "derived"
    // from Tables don't have those..
    // -----------------
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }


    @Override
    public Collection<ExasolTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Override
    public JDBCStructCache<ExasolSchema, ? extends DBSEntity, ? extends DBSEntityAttribute> getCache() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return SQLUtils.formatSQL(getDataSource(), this.text);

    }


}
