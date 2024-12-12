/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.SQLException;
import java.util.Map;

public class AltibasePackage extends GenericPackage implements DBPStatefulObject {

    protected String schemaName;
    private String source;

    private boolean valid; // 0: Valid, 1: Invalid
    private boolean hasBody;

    public AltibasePackage(GenericStructContainer container, String packageName, JDBCResultSet dbResult) {
        super(container, packageName, true);
        
        schemaName = container.getName();
        valid = JDBCUtils.safeGetBoolean(dbResult, "STATUS", "0");
    }

    public void setBody(boolean hasBody) {
        this.hasBody = hasBody;
    }
    
    public void setStatus(boolean valid) {
        this.valid = this.valid && valid;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            source = "-- Package specification " 
                    + AltibaseConstants.NEW_LINE 
                    + ((AltibaseMetaModel) getDataSource().getMetaModel())
                        .getPackageDDL(monitor, this, AltibaseConstants.PACKAGE_TYPE_SPEC)
                    + AltibaseConstants.NEW_LINE 
                    + "-- Package body " 
                    + AltibaseConstants.NEW_LINE;

            if (hasBody) {
                source += ((AltibaseMetaModel) getDataSource().getMetaModel())
                        .getPackageDDL(monitor, this, AltibaseConstants.PACKAGE_TYPE_BODY);
            } else {
                source += "-- No body definition";
            }
        }
        return source;
    }

    @Property(viewable = true, order = 5)
    public boolean isValid() {
        return valid;
    }
    
    @Override
    @Property(viewable = false, hidden = true, order = 3, labelProvider = GenericCatalog.CatalogNameTermProvider.class)
    public GenericCatalog getCatalog() {
        return super.getCatalog();
    }
    
    public void refreshState(JDBCSession session) throws DBCException {
        // Check package body status only because package spec. cannot be invalid.
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT status FROM system_.sys_users_ u, system_.sys_packages_ p"
                        + " WHERE p.package_type = 7 AND u.user_id = p.user_id AND u.user_name = ? AND package_name = ?")) {
            dbStat.setString(1, schemaName);
            dbStat.setString(2, getName());

            dbStat.executeStatement();

            try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                if (dbResult != null && dbResult.next()) {
                    valid = JDBCUtils.safeGetBoolean(dbResult, 1, "0"); // 0 is Valid, 1 is invalid
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of package '" + this.getName() + "'")) {
            refreshState(session);
        }
    }
    
    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, 
                "Refresh state of package '" + this.getName() + "'")) {
            refreshState(session);
        }
        
        return this;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }
}
