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
package org.jkiss.dbeaver.ui.editors.sql.semantics.context;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryQualifiedName;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.ui.editors.sql.semantics.model.SQLQueryRowsTableDataModel;

import java.util.*;
import java.util.stream.Collectors;


public class SQLQueryDummyDataSourceContext extends SQLQueryDataContext {

    private final SQLDialect dialect;

    private final DummyDbObject dummyDataSource;
    private final DummyDbObject defaultDummyCatalog;
    private final DummyDbObject defaultDummySchema;
    private final DummyDbObject defaultDummyTable;
    private final Set<String> knownColumnNames;
    private final Set<String> knownTableNames;
    private final Set<String> knownSchemaNames;
    private final Set<String> knownCatalogNames;


    @FunctionalInterface
    private static interface DummyObjectCtor {
        DummyDbObject apply(DummyDbObject parent, String name, int index);
    }
    
    private class DummyDbObject implements DBSEntityAttribute, DBSTable, DBSSchema, DBSCatalog, DBPDataSource, DBPImageProvider {
        private final DummyDbObject container;
        private final DBPImage image;
        private final String name;
        private final String description;
        private final int position;
        private final Set<String> childrenNames;
        private final DummyObjectCtor childCtor;
        private Map<String, DummyDbObject> childrenByName = null;
        private List<DummyDbObject> children = null;
        
        public DummyDbObject(
            @NotNull DummyDbObject container,
            @NotNull DBPImage image,
            @NotNull String name,
            @NotNull String description,
            int position,
            @NotNull Set<String> childrenNames,
            @Nullable DummyObjectCtor childCtor
        ) {
            this.container = container;
            this.image = image;
            this.name = name;
            this.description = description;
            this.position = position;
            this.childrenNames = childrenNames;
            this.childCtor = childCtor;
        }
        
        private Map<String, DummyDbObject> getChildrenMapImpl() {
            if (this.childrenByName == null) {
                if (this.childCtor == null) {
                    this.childrenByName = Collections.emptyMap();
                } else {
                    this.childrenByName = new HashMap<>();
                    int i = 0;
                    for (String name : this.childrenNames) {
                        this.childrenByName.put(name, childCtor.apply(this, name, i++));
                    }
                }
            }
            return this.childrenByName;
        }
        
        private List<DummyDbObject> getChildrenListImpl() {
            return this.children != null ? this.children : (this.children = new ArrayList<>(this.getChildrenMapImpl().values()));
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return this.container;
        }

        @Override
        public DBPDataSource getDataSource() {
            return dummyDataSource;
        }

        @Override
        public DBPImage getObjectImage() {
            return this.image;
        }
        
        @NotNull
        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public boolean isPersisted() {
            return true;
        }

        @Override
        public int getOrdinalPosition() {
            return this.position;
        }

        @Override
        public boolean isRequired() {
            return true;
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

        @Override
        public DBSEntityType getEntityType() {
            return DBSEntityType.TABLE;
        }

        @Override
        public List<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
            return this.getChildrenListImpl();
        }

        @Override
        public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
            return this.getChildrenMapImpl().get(attributeName);
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        @NotNull
        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            if (this.container == defaultDummySchema || this.container == defaultDummyCatalog || this.container == dummyDataSource) {
                return this.name;
            } else {
                return DBUtils.getFullQualifiedName(getDataSource(), this.container, this);
            }
        }

        @Override
        public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
            return this.getChildrenListImpl();
        }

        @Override
        public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
            return this.getChildrenMapImpl().get(childName);
        }

        @NotNull
        @Override
        public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
            return DummyDbObject.class;
        }

        @Override
        public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        }

        @Override
        public DBSInstance getDefaultInstance() {
            return null;
        }

        @NotNull
        @Override
        public Collection<? extends DBSInstance> getAvailableInstances() {
            return Collections.emptyList();
        }

        @Override
        public void shutdown(DBRProgressMonitor monitor) {
            
        }

        @Override
        public Map<String, ?> getContextAttributes() {
            return Collections.emptyMap();
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
            return dialect;
        }

        @Override
        public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
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
        public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public List<? extends DBSTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
            return Collections.emptyList();
        }
    }

    public SQLQueryDummyDataSourceContext(
        @NotNull SQLDialect dialect,
        @NotNull Set<String> knownColumnNames,
        @NotNull Set<List<String>> knownTableNames
    ) {
        this.dialect = dialect;
        this.knownColumnNames = knownColumnNames;
        this.knownTableNames = new HashSet<>();
        this.knownSchemaNames = new HashSet<>();
        this.knownCatalogNames = new HashSet<>();
        
        for (List<String> name : knownTableNames) {
            this.knownTableNames.add(name.get(name.size() - 1));
            if (name.size() > 1) {
                this.knownSchemaNames.add(name.get(name.size() - 2));
                if (name.size() > 2) {
                    this.knownCatalogNames.add(name.get(name.size() - 3));
                }
            }
        }
        
        if (this.knownCatalogNames.isEmpty()) {
            this.knownCatalogNames.add("dummyCatalog");
        }
        if (this.knownSchemaNames.isEmpty()) {
            this.knownSchemaNames.add("dummySchema");
        }
        
        this.dummyDataSource = this.prepareDataSource();
        this.defaultDummyCatalog = this.dummyDataSource.getChildrenMapImpl().values().stream().findFirst().get();
        this.defaultDummySchema = this.defaultDummyCatalog.getChildrenMapImpl().values().stream().findFirst().get();
        this.defaultDummyTable = this.prepareTable(this.defaultDummySchema, "", -1);
    }
    
    private DummyDbObject prepareDataSource() {
        return new DummyDbObject(
            null,
            UIIcon.DATABASES,
            "DummyDataSource",
            "Dummy data source for purposes of static query semantic analysis",
            0,
            knownCatalogNames,
            this::prepareCatalog
        );
    }
    
    private DummyDbObject prepareCatalog(DummyDbObject parent, String name, int index) {
        return new DummyDbObject(
            parent,
            DBIcon.TREE_DATABASE,
            name,
            "Dummy catalog for purposes of static query semantic analysis",
            index,
            knownSchemaNames,
            this::prepareSchema
        );
    }
    
    private DummyDbObject prepareSchema(DummyDbObject parent, String name, int index) {
        return new DummyDbObject(
            parent,
            DBIcon.TREE_SCHEMA,
            name,
            "Dummy schema for purposes of static query semantic analysis",
            index,
            knownTableNames,
            this::prepareTable
        );
    }
    
    private DummyDbObject prepareTable(DummyDbObject parent, String name, int index) {
        return new DummyDbObject(
            parent,
            DBIcon.TREE_TABLE,
            name,
            "Dummy table for purposes of static query semantic analysis",
            index,
            knownColumnNames,
            this::prepareColumn
        );
    }
    
    private DummyDbObject prepareColumn(DummyDbObject parent, String name, int index) {
        return new DummyDbObject(
            parent,
            DBIcon.TYPE_STRING,
            name,
            "Dummy column for purposes of static query semantic analysis",
            index,
            Collections.emptySet(),
            null
        );
    }
    
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return Collections.emptyList();
    }
    
    @Override
    public DBSEntity findRealTable(List<String> tableName) {
        List<String> rawTableName = tableName.stream().map(this.dialect::getUnquotedIdentifier).toList();
        DummyDbObject catalog = rawTableName.size() > 2
            ? this.dummyDataSource.getChildrenMapImpl().get(rawTableName.get(rawTableName.size() - 3)) : this.defaultDummyCatalog;
        DummyDbObject schema = rawTableName.size() > 1
            ? catalog.getChildrenMapImpl().get(rawTableName.get(rawTableName.size() - 2)) : this.defaultDummySchema;
        return schema.getChildrenMapImpl().get(rawTableName.get(rawTableName.size() - 1));
    }
    
    @Override
    public SQLQueryRowsSourceModel findRealSource(DBSEntity table) {
        return null;
    }
    
    @Override
    public SQLQueryResultColumn resolveColumn(String simpleName) {
        return null;
    }
    
    @Override
    public SQLDialect getDialect() {
        return this.dialect;
    }
    
    @NotNull
    @Override
    public SQLQueryRowsSourceModel getDefaultTable(@NotNull Interval range) {
        return new DummyTableRowsSource(range);
    }
    
    public class DummyTableRowsSource extends SQLQueryRowsTableDataModel {
        
        public DummyTableRowsSource(@NotNull Interval range) {
            super(range, new SQLQueryQualifiedName(new SQLQuerySymbolEntry(range, "DummyTable", "DummyTable")));
        }

        @Override
        public SQLQuerySymbolClass getSymbolClass() {
            return SQLQuerySymbolClass.TABLE;
        }

        @Override
        protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
            try {
                List<? extends DBSEntityAttribute> attributes = defaultDummyTable.getAttributes(new VoidProgressMonitor());
                if (attributes != null) {
                    List<SQLQueryResultColumn> columns = this.prepareResultColumnsList(context, attributes);
                    context = context.overrideResultTuple(columns);
                }
            } catch (DBException ex) {
                statistics.appendError(this.getName().entityName, "Failed to resolve table", ex);
            }
            return context;
        }
        
        @Override
        protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, @NotNull T node) {
            return visitor.visitDummyTableRowsSource(this, node);
        }
    }
    
}
