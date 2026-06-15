/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class KingbaseDatabase extends PostgreDatabase {

    private String characterType;

    private String databaseCompatibleMode;
    
    final LanguageCache languageCache = new LanguageCache();

    protected KingbaseDatabase(DBRProgressMonitor monitor, 
            KingbaseDataSource dataSource, 
            String name, 
            PostgreRole owner, 
            String templateName, 
            PostgreTablespace tablespace, 
            PostgreCharset encoding) throws DBException {
        super(monitor, dataSource, name, owner, templateName, tablespace, encoding);
    }

    protected KingbaseDatabase(DBRProgressMonitor monitor, 
            KingbaseDataSource dataSource, 
            String databaseName) throws DBException {
        super(monitor, dataSource, databaseName);
        readDatabaseInfo(monitor);
        checkInstanceConnection(monitor);
        
    }

    protected KingbaseDatabase(DBRProgressMonitor monitor, 
            KingbaseDataSource dataSource, 
            ResultSet dbResult) throws DBException {
        super(monitor, dataSource, dbResult);
        init(dbResult);
     
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource() {
        return (KingbaseDataSource) dataSource;
    }

    @Override
    @Property(viewable = true, order = 1)
    public long getObjectId() {
        return super.getObjectId();
    }

    @Property(viewable = true, order = 6)
    public String getCharacterType() {
        return this.characterType;
    }

    @Property(viewable = true, order = 7)
    public String getDatabaseCompatibleMode() {
        return this.databaseCompatibleMode;
    }


    private void init(ResultSet dbResult) {
        this.databaseCompatibleMode = JDBCUtils.safeGetString(dbResult, "datcompatibility");
        this.characterType = JDBCUtils.safeGetString(dbResult, "datctype");
    }

    public void setDatabaseCompatibleMode(String databaseCompatibleMode) {
        this.databaseCompatibleMode = databaseCompatibleMode;
    }

    public void readDatabaseInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = getMetaContext().openSession(monitor, DBCExecutionPurpose.META, "Load database info")) {
            try (JDBCPreparedStatement dbStat = session
                .prepareStatement("SELECT db.oid,db.* FROM sys_catalog.sys_database db WHERE datname=?")) {
                dbStat.setString(1, super.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.nextRow()) {
                        init(dbResult);
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }
    
    public Collection<PostgreLanguage> getLanguages(DBRProgressMonitor monitor) throws DBException {
        checkInstanceConnection(monitor);
        return languageCache.getAllObjects(monitor, this);
    }
    
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        languageCache.clearCache();
        return this;
    }
    
    public static class LanguageCache extends PostgreDatabaseJDBCObjectCache<PostgreLanguage> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, 
               @NotNull PostgreDatabase owner) throws SQLException {
            return session.prepareStatement(
                "SELECT l.oid,l.* FROM sys_catalog.sys_language l " +
                    "\nORDER BY l.oid"
            );
        }

        @Override
        protected PostgreLanguage fetchObject(
            @NotNull JDBCSession session,
            @NotNull PostgreDatabase owner,
            @NotNull JDBCResultSet dbResult
        ) throws SQLException {
            return new PostgreLanguage(owner, dbResult);
        }
    }

    @Override
    public KingbaseSchema createSchemaImpl(@NotNull PostgreDatabase owner, @NotNull String name,
        @NotNull JDBCResultSet resultSet) throws SQLException {
        return new KingbaseSchema(owner, name, resultSet);
    }

    @Override
    public KingbaseSchema createSchemaImpl(@NotNull PostgreDatabase owner, 
            @NotNull String name, 
            @Nullable PostgreRole postgreRole) {
        return new KingbaseSchema(owner, name, postgreRole);
    }
    
    
}
