/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import java.util.*;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

class SQLQueryFakeDataSourceContext extends SQLQueryDataContext {


    private final FakeSchema schema = new FakeSchema();
    private final Map<String, FakeTable> tables = new HashMap<>();
    private final List<FakeColumn> columns;

    private static class FakeSchema implements DBPDataSource {

        @Override
        public DBSInstance getDefaultInstance() {
            return null;
        }

        @Override
        public Collection<? extends DBSInstance> getAvailableInstances() {
            return Collections.emptyList();
        }

        @Override
        public void shutdown(DBRProgressMonitor monitor) {
        }

        @Override
        public DBSObject getParentObject() {
            return null;
        }

        @Override
        public DBPDataSource getDataSource() {
            return this;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getDescription() {
            return "Fake data schema for databaseless query analysis purposes";
        }

        @Override
        public boolean isPersisted() {
            return true;
        }

        @Override
        public Map<String, ?> getContextAttributes() {
            return null;
        }

        @Override
        public <T> T getContextAttribute(String attributeName) {
            return null;
        }

        @Override
        public <T> void setContextAttribute(String attributeName, T attributeValue) {
        }

        @Override
        public void removeContextAttribute(String attributeName) {
        }

        @Override
        public DBPDataSourceContainer getContainer() {
            return null;
        }

        @Override
        public DBPDataSourceInfo getInfo() {
            return null;
        }

        @Override
        public Object getDataSourceFeature(String featureId) {
            return null;
        }

        @Override
        public SQLDialect getSQLDialect() {
            return BasicSQLDialect.INSTANCE;
        }

        @Override
        public void initialize(DBRProgressMonitor monitor) throws DBException {
        }
        
    }
    
    private static class FakeTable implements DBSTable {
        public final String name;
        public final FakeSchema schema;

        public FakeTable(FakeSchema schema, String name) {
            this.schema = schema;
            this.name = name;
        }

        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TABLE;
        }

        @Override
        public List<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
            return null;
        }

        @Override
        public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public DBSObject getParentObject() {
            return this.schema;
        }

        @Override
        public DBPDataSource getDataSource() {
            return this.schema;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDescription() {
            return "Fake table for databaseless query analysis purposes";
        }

        @Override
        public boolean isPersisted() {
            return true;
        }

        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            return this.name;
        }

        @Override
        public boolean isView() {
            return false;
        }

        @Override
        public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public List<? extends DBSTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }
    }
    
    class FakeColumn implements DBSEntityAttribute {
        
        public final FakeTable table;
        public final String name;
        
        public FakeColumn(FakeTable table, String name) {
            this.table = table;
            this.name = name;
        }

        @Override
        public DBSEntity getParentObject() {
            return this.table;
        }

        @Override
        public DBPDataSource getDataSource() {
            return this.table.getDataSource();
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public boolean isPersisted() {
            return false;
        }

        @Override
        public int getOrdinalPosition() {
            return 0;
        }

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public boolean isAutoGenerated() {
            return false;
        }

        @Override
        public String getTypeName() {
            return null;
        }

        @Override
        public String getFullTypeName() {
            return null;
        }

        @Override
        public int getTypeID() {
            return 0;
        }

        @Override
        public DBPDataKind getDataKind() {
            return DBPDataKind.STRING;
        }

        @Override
        public Integer getScale() {
            return null;
        }

        @Override
        public Integer getPrecision() {
            return null;
        }

        @Override
        public long getMaxLength() {
            return 0;
        }

        @Override
        public long getTypeModifiers() {
            return 0;
        }

        @Override
        public String getDefaultValue() {
            return null;
        }
        
    }
    
    public SQLQueryFakeDataSourceContext(List<SQLQueryColumnReferenceExpression> knownColumns) {
        this.tables.put("", new FakeTable(schema, ""));
        this.columns = knownColumns.stream().map(c -> new FakeColumn(tables.get(""), c.getColumnNameIfTrivialExpression().getName())).toList();
    }
    
    @Override
    public List<SQLQuerySymbol> getColumnsList() {
        return Collections.emptyList(); // TODO: return columns
    }
    
    @Override
    public DBSTable findRealTable(List<String> tableName) {
        String name = !tableName.isEmpty() ? tableName.get(tableName.size() - 1) : null;
        return name == null ? null : this.tables.computeIfAbsent(tableName.get(0), s -> new FakeTable(schema, s));
    }
    
    @Override
    public SQLQueryRowsSource findRealSource(DBSTable table) {
        return null;
    }
    
    @Override
    public SQLQuerySymbolDefinition resolveColumn(String simpleName) {
        return new SQLQuerySymbolByDbObjectDefinition(null, SQLQuerySymbolClass.COLUMN);
    }
}
