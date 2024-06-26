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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBStructUtils;

import java.util.Map;


public abstract class MySQLTableConstraintBase extends JDBCTableConstraint<MySQLTable, MySQLTableConstraintColumn> implements MySQLSourceObject {

    public MySQLTableConstraintBase(MySQLTable table, String name, String description, DBSEntityConstraintType constraintType, boolean persisted) {
        super(table, name, description, constraintType, persisted);
    }

    public MySQLTableConstraintBase(MySQLTable table, String name, String description, DBSEntityConstraintType constraintType, boolean persisted, JDBCResultSet resultSet) {
        super(table, name, description, constraintType, persisted);
    }

    public MySQLTableConstraintBase(MySQLTable table, DBSEntityConstraint constraintSource, boolean persisted) {
        super(table, constraintSource, persisted);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getTable().getContainer(),
                getTable(),
                this);
    }

    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Constraints DDL is read-only");
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateObjectDDL(monitor, this, options, false);
    }
}
