/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
