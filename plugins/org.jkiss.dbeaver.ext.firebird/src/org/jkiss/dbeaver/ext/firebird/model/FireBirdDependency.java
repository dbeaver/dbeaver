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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An object another object depends on, or an object that depends on another object, read from RDB$DEPENDENCIES.
 * Internal objects are shown as the object they implement: a CHECK_n trigger as its check constraint,
 * an RDB$n field as its computed column.
 */
public class FireBirdDependency implements DBSObject, DBPImageProvider {

    // Object types of RDB$DEPENDENCIES (RDB$TYPES where RDB$FIELD_NAME = 'RDB$OBJECT_TYPE')
    public static final int TYPE_TABLE = 0;
    public static final int TYPE_VIEW = 1;
    public static final int TYPE_TRIGGER = 2;
    public static final int TYPE_COMPUTED_FIELD = 3;
    public static final int TYPE_VALIDATION = 4;
    public static final int TYPE_PROCEDURE = 5;
    public static final int TYPE_INDEX_EXPRESSION = 6;
    public static final int TYPE_EXCEPTION = 7;
    public static final int TYPE_USER = 8;
    public static final int TYPE_DOMAIN = 9;
    public static final int TYPE_INDEX = 10;
    public static final int TYPE_CHARACTER_SET = 11;
    public static final int TYPE_ROLE = 13;
    public static final int TYPE_GENERATOR = 14;
    /** UDFs and PSQL functions (Firebird 3+) */
    public static final int TYPE_FUNCTION = 15;
    public static final int TYPE_BLOB_FILTER = 16;
    public static final int TYPE_COLLATION = 17;
    public static final int TYPE_PACKAGE = 18;
    public static final int TYPE_PACKAGE_BODY = 19;
    public static final int TYPE_INDEX_CONDITION = 37;

    /**
     * Tables and views are both relations. A view may be referenced with either type, e.g. by a trigger on it.
     */
    public static final int[] RELATION_TYPES = {TYPE_TABLE, TYPE_VIEW};

    /**
     * Kind of the dependency as shown in the navigator. Internal objects of RDB$DEPENDENCIES
     * (computed fields, CHECK triggers, index expressions) are mapped to the objects they belong to.
     */
    public enum Kind {
        TABLE("Table", DBIcon.TREE_TABLE),
        VIEW("View", DBIcon.TREE_VIEW),
        TABLE_TRIGGER("Table trigger", DBIcon.TREE_TRIGGER),
        DATABASE_TRIGGER("Database trigger", DBIcon.TREE_TRIGGER),
        CHECK_CONSTRAINT("Check constraint", DBIcon.TREE_CONSTRAINT),
        COMPUTED_COLUMN("Computed column", DBIcon.TREE_COLUMN),
        PROCEDURE("Procedure", DBIcon.TREE_PROCEDURE),
        FUNCTION("Function", DBIcon.TREE_FUNCTION),
        INDEX("Index", DBIcon.TREE_INDEX),
        EXCEPTION("Exception", DBIcon.TREE_REFERENCE),
        DOMAIN("Domain", DBIcon.TREE_DATA_TYPE),
        SEQUENCE("Sequence", DBIcon.TREE_SEQUENCE),
        PACKAGE("Package", DBIcon.TREE_PACKAGE),
        PACKAGE_BODY("Package body", DBIcon.TREE_PACKAGE),
        COLLATION("Collation", DBIcon.TREE_REFERENCE),
        OTHER("Other", DBIcon.TREE_REFERENCE);

        private final String title;
        private final DBPImage icon;

        Kind(@NotNull String title, @NotNull DBPImage icon) {
            this.title = title;
            this.icon = icon;
        }

        @NotNull
        public String getTitle() {
            return title;
        }
    }

    @NotNull
    private final DBSObject owner;
    @NotNull
    private final String name;
    @NotNull
    private final Kind kind;
    @Nullable
    private final String tableName;
    private final Set<String> fieldNames = new LinkedHashSet<>();
    @Nullable
    private DBSObject targetObject;
    private boolean targetResolved;

    FireBirdDependency(@NotNull DBSObject owner, @NotNull String name, @NotNull Kind kind, @Nullable String tableName) {
        this.owner = owner;
        this.name = name;
        this.kind = kind;
        this.tableName = tableName;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public String getObjectType() {
        return kind.getTitle();
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    /**
     * Table of a trigger, check constraint, computed column or index
     */
    @Nullable
    @Property(viewable = true, order = 3)
    public String getTableName() {
        return tableName;
    }

    /**
     * Columns used by the dependency, empty if it uses the object as a whole
     */
    @NotNull
    @Property(viewable = true, order = 4)
    public String getFieldNames() {
        return String.join(", ", fieldNames);
    }

    /**
     * The object itself, if DBeaver knows its type
     */
    @Nullable
    @Property(viewable = true, order = 5)
    public DBSObject getTargetObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!targetResolved) {
            targetObject = findTargetObject(monitor);
            targetResolved = true;
        }
        return targetObject;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return owner.getDataSource();
    }

    @NotNull
    @Override
    public DBPImage getObjectImage() {
        return kind.icon;
    }

    @Override
    public String toString() {
        return kind.getTitle() + " " + (tableName == null ? name : tableName + "." + name);
    }

    @Nullable
    private DBSObject findTargetObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!(owner.getDataSource() instanceof GenericDataSource dataSource)) {
            return null;
        }
        GenericTableBase table = tableName == null ? null : dataSource.getTable(monitor, tableName);
        return switch (kind) {
            case TABLE, VIEW -> dataSource.getTable(monitor, name);
            case PROCEDURE, FUNCTION -> DBUtils.findObject(dataSource.getProcedures(monitor), name);
            case SEQUENCE -> DBUtils.findObject(dataSource.getSequences(monitor), name);
            case DOMAIN -> dataSource.getLocalDataType(name);
            case DATABASE_TRIGGER -> DBUtils.findObject(dataSource.getTriggers(monitor), name);
            case TABLE_TRIGGER -> table == null ? null : DBUtils.findObject(table.getTriggers(monitor), name);
            case CHECK_CONSTRAINT -> table == null ? null : DBUtils.findObject(table.getConstraints(monitor), name);
            case COMPUTED_COLUMN -> table == null ? null : table.getAttribute(monitor, name);
            case INDEX -> table == null ? null : DBUtils.findObject(table.getIndexes(monitor), name);
            default -> null;
        };
    }

    /**
     * Reads the objects the given object depends on, or the objects that depend on it.
     *
     * @param owner       object whose dependencies are read
     * @param objectTypes types of the object in RDB$DEPENDENCIES, e.g. both table and view for a relation
     * @param dependents  true for the objects that depend on the given object ("Used by"),
     *                    false for the objects the given object depends on ("Depends on")
     */
    @NotNull
    public static List<FireBirdDependency> readDependencies(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject owner,
        @NotNull int[] objectTypes,
        boolean dependents
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Load object dependencies")) {
            return readDependencies(session, owner, objectTypes, dependents);
        } catch (SQLException e) {
            throw new DBException("Error reading dependencies of " + owner.getName(), e);
        }
    }

    @NotNull
    static List<FireBirdDependency> readDependencies(
        @NotNull JDBCSession session,
        @NotNull DBSObject owner,
        @NotNull int[] objectTypes,
        boolean dependents
    ) throws SQLException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(getDependenciesQuery(objectTypes, dependents))) {
            dbStat.setString(1, owner.getName());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                // RDB$DEPENDENCIES has a row per used column; collect them into one dependency per object
                Map<String, FireBirdDependency> dependencies = new LinkedHashMap<>();
                while (dbResult.next()) {
                    FireBirdDependency dependency = createDependency(owner, dbResult);
                    if (dependency == null) {
                        continue;
                    }
                    FireBirdDependency existing = dependencies.putIfAbsent(dependency.toString(), dependency);
                    String fieldName = JDBCUtils.safeGetStringTrimmed(dbResult, "FIELD_NAME");
                    if (CommonUtils.isNotEmpty(fieldName)) {
                        (existing == null ? dependency : existing).fieldNames.add(fieldName);
                    }
                }
                return new ArrayList<>(dependencies.values());
            }
        }
    }

    /**
     * The query reads one side of RDB$DEPENDENCIES and resolves internal objects to the objects they belong to:
     * computed fields to their column, CHECK triggers to their constraint and index expressions to their table.
     */
    @NotNull
    static String getDependenciesQuery(@NotNull int[] objectTypes, boolean dependents) {
        String objName = dependents ? "D.RDB$DEPENDENT_NAME" : "D.RDB$DEPENDED_ON_NAME";
        String objType = dependents ? "D.RDB$DEPENDENT_TYPE" : "D.RDB$DEPENDED_ON_TYPE";
        String ownerName = dependents ? "D.RDB$DEPENDED_ON_NAME" : "D.RDB$DEPENDENT_NAME";
        String ownerType = dependents ? "D.RDB$DEPENDED_ON_TYPE" : "D.RDB$DEPENDENT_TYPE";
        String types = Arrays.stream(objectTypes).mapToObj(String::valueOf).collect(Collectors.joining(","));
        return "SELECT " + objName + " AS OBJECT_NAME," + objType + " AS OBJECT_TYPE,D.RDB$FIELD_NAME AS FIELD_NAME,\n" +
            "COALESCE(RF.RDB$RELATION_NAME,TR.RDB$RELATION_NAME,I.RDB$RELATION_NAME) AS TABLE_NAME,\n" +
            "RF.RDB$FIELD_NAME AS COLUMN_NAME,RC.RDB$CONSTRAINT_NAME AS CONSTRAINT_NAME\n" +
            "FROM RDB$DEPENDENCIES D\n" +
            "LEFT JOIN RDB$RELATION_FIELDS RF ON " + objType + "=" + TYPE_COMPUTED_FIELD +
            " AND RF.RDB$FIELD_SOURCE=" + objName + "\n" +
            "LEFT JOIN RDB$TRIGGERS TR ON " + objType + "=" + TYPE_TRIGGER + " AND TR.RDB$TRIGGER_NAME=" + objName + "\n" +
            "LEFT JOIN RDB$CHECK_CONSTRAINTS CC ON " + objType + "=" + TYPE_TRIGGER + " AND CC.RDB$TRIGGER_NAME=" + objName + "\n" +
            "LEFT JOIN RDB$RELATION_CONSTRAINTS RC ON RC.RDB$CONSTRAINT_NAME=CC.RDB$CONSTRAINT_NAME" +
            " AND RC.RDB$CONSTRAINT_TYPE='CHECK'\n" +
            "LEFT JOIN RDB$INDICES I ON " + objType + " IN (" + TYPE_INDEX_EXPRESSION + "," + TYPE_INDEX + "," +
            TYPE_INDEX_CONDITION + ") AND I.RDB$INDEX_NAME=" + objName + "\n" +
            "WHERE " + ownerName + "=? AND " + ownerType + " IN (" + types + ")\n" +
            "ORDER BY 2,1,3";
    }

    @Nullable
    private static FireBirdDependency createDependency(@NotNull DBSObject owner, @NotNull JDBCResultSet dbResult) {
        String objectName = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_NAME");
        if (CommonUtils.isEmpty(objectName)) {
            return null;
        }
        int objectType = JDBCUtils.safeGetInt(dbResult, "OBJECT_TYPE");
        String tableName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABLE_NAME");
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        String constraintName = JDBCUtils.safeGetStringTrimmed(dbResult, "CONSTRAINT_NAME");
        return switch (objectType) {
            case TYPE_TABLE -> new FireBirdDependency(owner, objectName, Kind.TABLE, null);
            case TYPE_VIEW -> new FireBirdDependency(owner, objectName, Kind.VIEW, null);
            case TYPE_TRIGGER -> {
                if (constraintName != null) {
                    yield new FireBirdDependency(owner, constraintName, Kind.CHECK_CONSTRAINT, tableName);
                }
                yield tableName == null
                    ? new FireBirdDependency(owner, objectName, Kind.DATABASE_TRIGGER, null)
                    : new FireBirdDependency(owner, objectName, Kind.TABLE_TRIGGER, tableName);
            }
            case TYPE_COMPUTED_FIELD -> columnName == null
                ? new FireBirdDependency(owner, objectName, Kind.OTHER, null)
                : new FireBirdDependency(owner, columnName, Kind.COMPUTED_COLUMN, tableName);
            case TYPE_PROCEDURE -> new FireBirdDependency(owner, objectName, Kind.PROCEDURE, null);
            case TYPE_FUNCTION -> new FireBirdDependency(owner, objectName, Kind.FUNCTION, null);
            case TYPE_INDEX_EXPRESSION, TYPE_INDEX, TYPE_INDEX_CONDITION ->
                new FireBirdDependency(owner, objectName, Kind.INDEX, tableName);
            case TYPE_EXCEPTION -> new FireBirdDependency(owner, objectName, Kind.EXCEPTION, null);
            case TYPE_DOMAIN -> new FireBirdDependency(owner, objectName, Kind.DOMAIN, null);
            case TYPE_GENERATOR -> new FireBirdDependency(owner, objectName, Kind.SEQUENCE, null);
            case TYPE_PACKAGE -> new FireBirdDependency(owner, objectName, Kind.PACKAGE, null);
            case TYPE_PACKAGE_BODY -> new FireBirdDependency(owner, objectName, Kind.PACKAGE_BODY, null);
            case TYPE_COLLATION -> new FireBirdDependency(owner, objectName, Kind.COLLATION, null);
            default -> new FireBirdDependency(owner, objectName, Kind.OTHER, null);
        };
    }
}
