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
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

public class ExasolView extends ExasolTableBase implements ExasolSourceObject {


    private String owner;
    private Boolean hasRead = false;
    

    private String text;

    public ExasolView(ExasolSchema schema, String name, boolean persisted) {
        super(schema, name, persisted);
    }

    public ExasolView(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        hasRead=false;

    }
    
    public ExasolView(ExasolSchema schema)
    {
        super(schema,null,false);
        text = "";
        hasRead = true;
    }


    @Override
    public DBSObjectState getObjectState() {
        return DBSObjectState.NORMAL;
    }


    @Override
    @Property(viewable = true, editable = false, updatable = false, order = 40)
    public String getDescription() {
        return super.getDescription();
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Property(viewable = true, order = 100)
    public String getOwner() throws DBCException {
        read();
        return owner;
    }

    private void read() throws DBCException
    {
        if (!hasRead)
        {
            JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), getDataSource(), "Read Table Details");
            try (JDBCStatement stmt = session.createStatement())
            {
                String sql = String.format("SELECT VIEW_OWNER,VIEW_TEXT FROM SYS.EXA_ALL_VIEWS WHERE VIEW_SCHEMA = '%s' and VIEW_NAME = '%s'",
                        ExasolUtils.quoteString(this.getSchema().getName()),
                        ExasolUtils.quoteString(this.getName())
                        );
                
                try (JDBCResultSet dbResult = stmt.executeQuery(sql)) 
                {
                    Boolean read = dbResult.next();
                    
                    if (read) {
                        this.owner = JDBCUtils.safeGetString(dbResult, "VIEW_OWNER");
                        this.text = JDBCUtils.safeGetString(dbResult, "VIEW_TEXT");
                        this.hasRead = true;
                    } else {
                        this.owner = "SYS OBJECT";
                        this.text = "No View Text for system objects available";
                    }
                    this.hasRead = true;
                }
                
            } catch (SQLException e) {
                throw new DBCException(e,getDataSource());
            }
            
        }
        
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
        
        //force reading of attributes
        hasRead = false;
        return this;
    }


    // -----------------
    // Associations (Imposed from DBSTable). In Exasol, Most of objects "derived"
    // from Tables don't have those..
    // -----------------
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }


    @Override
    public Collection<ExasolTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        read();
        return SQLUtils.formatSQL(getDataSource(), this.text);

    }
    
    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        this.text = sourceText;
    }
    
    public String getSource() throws DBCException
    {
        read();
        return this.text;
    }

    
    @Override
    public JDBCStructCache<ExasolSchema, ExasolView, ExasolTableColumn> getCache() {
        return getContainer().getViewCache();
    }
    
}
