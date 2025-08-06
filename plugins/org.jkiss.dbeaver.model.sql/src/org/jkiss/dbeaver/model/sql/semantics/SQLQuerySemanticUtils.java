/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.context.*;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableDataModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.utils.Pair;

import java.lang.reflect.AccessFlag;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLQuerySemanticUtils {

    private static final Log log = Log.getLog(SQLQuerySemanticUtils.class);

    private SQLQuerySemanticUtils() {

    }

    /**
     * Resolve object and origin from name parts
     */
    public static void performPartialResolution(
        @NotNull SQLQueryRowsSourceContext context,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull SQLQueryComplexName name,
        @NotNull SQLQuerySymbolOrigin origin,
        @NotNull Set<DBSObjectType> objectTypes,
        @NotNull SQLQuerySymbolClass entityNameClass
    ) {
        if (!statistics.useRealMetadata() || context.getConnectionInfo().isDummy()) {
            return;
        }

        int namePartsCount = name.invalidPartsCount == 0 ? name.parts.size() : name.parts.indexOf(null);

        DBSObject object = null;
        List<String> fragmentStrings = null;
        for (int len = namePartsCount; len > 0 && object == null; len--) {
            fragmentStrings = name.stringParts.subList(0, len);
            List<? extends DBSObject> objs = context.getConnectionInfo()
                .findRealObjects(statistics.getMonitor(), RelationalObjectType.TYPE_UNKNOWN, fragmentStrings).stream()
                .filter(o -> objectTypes.stream().anyMatch(t -> t.getTypeClass().isAssignableFrom(o.getClass())))
                .toList();
            if (objs.size() == 1) {
                object = objs.getFirst();
            } else if (objs.size() > 1) {
                SQLQuerySymbolEntry ambiguousEntry = name.parts.get(len - 1);
                ambiguousEntry.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                Set<String> names = objs.stream().map(SQLQuerySemanticUtils::getObjectUniqueName).collect(Collectors.toSet());
                statistics.appendWarning(ambiguousEntry, "Ambiguous identifier: " + String.join(" vs ", names));
            }
        }

        if (object != null && !fragmentStrings.isEmpty()) {
            List<SQLQuerySymbolEntry> nameFragment = name.parts.subList(0, fragmentStrings.size());
            setNamePartsDefinition(nameFragment, object, inferSymbolClass(object), origin);
            if (name.parts.size() > nameFragment.size()) {
                SQLQuerySymbolEntry part = name.parts.get(nameFragment.size());
                SQLQuerySymbolOrigin lastPartOrigin = new SQLQuerySymbolOrigin.DbObjectFromDbObject(object, objectTypes);
                if (part != null) {
                    part.setOrigin(lastPartOrigin);
                } else if (name.parts.size() == nameFragment.size() + 1 && name.endingPeriodNode != null) {
                    name.endingPeriodNode.setOrigin(lastPartOrigin);
                }
            }
        } else if (!name.parts.isEmpty()) {
            name.parts.getFirst().setOrigin(origin);
        }

        if (name.parts.getLast() != null && name.parts.getLast().isNotClassified()) {
            name.parts.getLast().getSymbol().setSymbolClass(entityNameClass);
        }
    }

    public static void setNamePartsDefinition(
        @NotNull SQLQueryComplexName name,
        @NotNull DBSObject realObject,
        @NotNull SQLQuerySymbolOrigin origin
    ) {
        SQLQuerySymbolClass entityNameClass  = inferSymbolClass(realObject);
        setNamePartsDefinition(name,  realObject, entityNameClass, origin);
    }

    public static void setNamePartsDefinition(
        @NotNull SQLQueryComplexName name,
        @NotNull DBSObject realObject,
        @NotNull SQLQuerySymbolClass entityNameClass,
        @NotNull SQLQuerySymbolOrigin origin
    ) {
        setNamePartsDefinition(name.parts, realObject, entityNameClass, origin);
    }

    private static void setNamePartsDefinition(
        @NotNull List<SQLQuerySymbolEntry> parts,
        @NotNull DBSObject realObject,
        @NotNull SQLQuerySymbolClass entityNameClass,
        @NotNull SQLQuerySymbolOrigin origin
    ) {
        if (!parts.isEmpty()) {
            SQLQuerySymbolEntry lastPart = parts.getLast();
            lastPart.setDefinition(new SQLQuerySymbolByDbObjectDefinition(realObject, entityNameClass));
            DBSObject object = realObject.getParentObject();
            int scopeNameIndex = parts.size() - 2;
            while (object != null && scopeNameIndex >= 0) {
                SQLQuerySymbolEntry nameEntry = parts.get(scopeNameIndex);
                String objectName = SQLUtils.identifierToCanonicalForm(
                    object.getDataSource().getSQLDialect(),
                    DBUtils.getQuotedIdentifier(object),
                    false,
                    true
                );
                if (objectName.equalsIgnoreCase(nameEntry.getName())) {
                    SQLQuerySymbolClass objectNameClass = inferSymbolClass(object);
                    nameEntry.setDefinition(new SQLQuerySymbolByDbObjectDefinition(object, objectNameClass));
                    lastPart.setOrigin(new SQLQuerySymbolOrigin.DbObjectFromDbObject(object, RelationalObjectType.TYPE_UNKNOWN));
                    lastPart = nameEntry;
                    scopeNameIndex--;
                }
                object = object.getParentObject();
            }
            lastPart.setOrigin(origin);
        }
    }

    @NotNull
    public static SQLQuerySymbolClass inferSymbolClass(@NotNull DBSObject object) {
        SQLQuerySymbolClass objectNameClass;
        if (object instanceof DBSTable || object instanceof DBSView) {
            objectNameClass = SQLQuerySymbolClass.TABLE;
        } else if (object instanceof DBSSchema) {
            objectNameClass = SQLQuerySymbolClass.SCHEMA;
        } else if (object instanceof DBSCatalog) {
            objectNameClass = SQLQuerySymbolClass.CATALOG;
        } else {
            objectNameClass = SQLQuerySymbolClass.UNKNOWN; // TODO consider OBJECT is not necessarily TABLE
        }
        return objectNameClass;
    }

    @NotNull
    private static SQLQuerySymbol prepareColumnSymbol(@NotNull SQLDialect dialect, @NotNull DBSEntityAttribute attr) {
        String name = SQLUtils.identifierToCanonicalForm(dialect, attr.getName(), false, true);
        SQLQuerySymbol symbol = new SQLQuerySymbol(name);
        symbol.setDefinition(new SQLQuerySymbolByDbObjectDefinition(attr, SQLQuerySymbolClass.COLUMN));
        return symbol;
    }

    /**
     * Returns row tuple columns based on the attributes obtained from the table referenced in the query
     */
    @NotNull
    public static Pair<List<SQLQueryResultColumn>, List<SQLQueryResultPseudoColumn>> prepareResultColumnsList(
        @NotNull STMTreeNode cause,
        @NotNull SQLQueryRowsSourceModel rowsSourceModel,
        @Nullable DBSEntity table,
        @NotNull SQLDialect dialect,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull List<? extends DBSEntityAttribute> attributes
    ) {
        List<SQLQueryResultColumn> columns = new ArrayList<>(attributes.size());
        List<SQLQueryResultPseudoColumn> pseudoColumns = new ArrayList<>(attributes.size());
        for (DBSEntityAttribute attr : attributes) {
            if (DBUtils.isHiddenObject(attr)) {
                pseudoColumns.add(new SQLQueryResultPseudoColumn(
                    prepareColumnSymbol(dialect, attr),
                    rowsSourceModel,
                    table,
                    obtainColumnType(cause, statistics, attr),
                    DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL,
                    attr.getDescription()
                ));
            } else {
                columns.add(new SQLQueryResultColumn(
                    columns.size(),
                    prepareColumnSymbol(dialect, attr),
                    rowsSourceModel, table, attr,
                    obtainColumnType(cause, statistics, attr)
                ));
            }
        }
        return Pair.of(columns, pseudoColumns);
    }

    @NotNull
    private static SQLQueryExprType obtainColumnType(
        @NotNull STMTreeNode reason,
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull DBSAttributeBase attr
    ) {
        SQLQueryExprType type;
        try {
            type = SQLQueryExprType.forTypedObject(statistics.getMonitor(), attr, SQLQuerySymbolClass.COLUMN);
        } catch (DBException e) {
            log.debug(e);
            statistics.appendError(reason, "Failed to resolve column type for column " + attr.getName(), e);
            type = SQLQueryExprType.UNKNOWN;
        }
        return type;
    }

    public static List<SQLQueryResultPseudoColumn> prepareResultPseudoColumnsList(
        @NotNull SQLDialect dialect,
        @Nullable SQLQueryRowsSourceModel source,
        @Nullable DBSEntity table,
        @NotNull Stream<DBDPseudoAttribute> pseudoAttributes
    ) {
        return pseudoAttributes.map(a -> new SQLQueryResultPseudoColumn(
            new SQLQuerySymbol(SQLUtils.identifierToCanonicalForm(dialect, a.getName(), false, false)),
            source, table, SQLQueryExprType.UNKNOWN, a.getPropagationPolicy(), a.getDescription()
        )).collect(Collectors.toList());
    }

    public static void setNamePartsDefinition(
        @NotNull SQLQueryComplexName name,
        @NotNull SourceResolutionResult rr,
        @NotNull SQLQuerySymbolOrigin origin
    ) {
        name.parts.getFirst().setOrigin(origin);
        if (rr.aliasOrNull != null && name.parts.size() == 1) {
            name.parts.getFirst().setDefinition(rr.aliasOrNull.getDefinition());
        } else if (rr.source instanceof SQLQueryRowsTableDataModel tableModel) {
            SQLQueryComplexName tableName = tableModel.getName();
            if (tableName != null) {
                SQLQuerySymbolEntry lastDefSymbolEntry = tableName.parts.getLast();
                int i = name.parts.size() - 1;
                int j = tableName.parts.size() - 1;
                for (; i >= 0 && j >= 0; i--, j--) {
                    SQLQuerySymbolEntry part = name.parts.get(i);
                    part.setDefinition(lastDefSymbolEntry = tableName.parts.get(j));
                    if (part.getOrigin() == null) {
                        part.setOrigin(lastDefSymbolEntry.getOrigin());
                    }
                }
                while (i >= 0) {
                    SQLQuerySymbolEntry part = name.parts.get(i);
                    if (part.getOrigin() == null) {
                        part.setDefinition(lastDefSymbolEntry);
                    }
                    i--;
                }
            }
        } else {
            throw new IllegalStateException("Failed to propagate entity reference definition for " + name.getNameString());
        }
    }

    /**
     * Propagate semantics context and establish relations through the query model for column definition
     */
    public static void propagateColumnDefinition(
        @NotNull SQLQuerySymbolEntry columnName,
        @Nullable SQLQueryResultColumn resultColumn,
        @NotNull SQLQueryRecognitionContext statistics,
        @Nullable SQLQuerySymbolOrigin columnNameOrigin
    ) {
        // TODO consider ambiguity
        if (resultColumn != null) {
            columnName.setDefinition(resultColumn.symbol.getDefinition());
        } else {
            columnName.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(columnName, "Column " + columnName.getName() + " not found");
        }
        columnName.setOrigin(columnNameOrigin);
    }

    @Nullable
    public static SQLQueryExprType tryResolveMemberReference(
        @NotNull SQLQueryRecognitionContext statistics,
        @NotNull SQLQueryExprType valueType,
        @NotNull SQLQuerySymbolEntry identifier,
        @NotNull SQLQuerySymbolOrigin memberOrigin
    ) {
        identifier.setOrigin(memberOrigin);

        SQLQueryExprType type;
        try {
            type = valueType.findNamedMemberType(statistics.getMonitor(), identifier.getName());

            if (type != null) {
                identifier.setDefinition(type.getDeclaratorDefinition());
            } else {
                identifier.getSymbol().setSymbolClass(SQLQuerySymbolClass.ERROR);
                statistics.appendError(
                    identifier,
                    "Failed to resolve member reference " + identifier.getName() + " for " + valueType.getDisplayName()
                );
            }
        } catch (DBException e) {
            log.debug(e);
            statistics.appendError(
                identifier,
                "Failed to resolve member reference " + identifier.getName() + " for " + valueType.getDisplayName(),
                e
            );
            type = null;
        }
        return type;
    }

    @Nullable
    public static SQLQuerySymbolClass getIdentifierSymbolClass(@Nullable SQLQuerySymbol symbol) {
        return symbol == null ? null : symbol.getSymbolClass();
    }

    @Nullable
    public static SQLQuerySymbolClass getIdentifierSymbolClass(@Nullable SQLQuerySymbolEntry entry) {
        return entry == null ? null : entry.getSymbolClass();
    }

    @Nullable
    public static SQLQuerySymbolClass getIdentifierSymbolClass(@Nullable SQLQueryComplexName name) {
        if (name == null) {
            return null;
        } else if (name.parts.isEmpty()) {
            return null;
        } else if (name.parts.size() == 1) {
            SQLQuerySymbolEntry part = name.parts.getFirst();
            return part == null ? null : part.getSymbolClass();
        } else {
            for (int i = name.parts.size() - 1; i >= 0; i--) {
                SQLQuerySymbolEntry part = name.parts.get(i);
                if (part != null) {
                    return part.getSymbolClass();
                }
            }
            return null;
        }
    }

    private static final Set<DBSObjectType> knownObjectTypes = Set.of(
        RelationalObjectType.TYPE_TABLE,
        RelationalObjectType.TYPE_SCHEMA,
        RelationalObjectType.TYPE_CATALOG,
        RelationalObjectType.TYPE_VIEW,
        RelationalObjectType.TYPE_TABLE_COLUMN,
        RelationalObjectType.TYPE_INDEX,
        RelationalObjectType.TYPE_CONSTRAINT,
        RelationalObjectType.TYPE_PROCEDURE,
        RelationalObjectType.TYPE_SEQUENCE,
        RelationalObjectType.TYPE_TRIGGER,
        RelationalObjectType.TYPE_DATA_TYPE,
        RelationalObjectType.TYPE_PACKAGE,
        RelationalObjectType.TYPE_SYNONYM
    );

    @Nullable
    private static String inferObjectTypeName(@NotNull DBSObject object) {
        return knownObjectTypes.stream()
            .filter(t -> t.getTypeClass().isAssignableFrom(object.getClass())).findFirst()
            .map(DBSObjectType::getTypeName)
            .orElse(null);
    }

    @Nullable
    public static String getObjectTypeName(@NotNull DBSObject object) {
        String typeName;
        if (SQLQueryConnectionDummyContext.isDummyObject(object)) {
            typeName = inferObjectTypeName(object);
        } else {
            typeName = DBUtils.getObjectTypeName(object);
            if (typeName.equalsIgnoreCase("Object")) {
                typeName = inferObjectTypeName(object);
            }
        }
        return typeName;
    }

    @NotNull
    public static String getObjectUniqueName(@NotNull DBSObject o) {
        return o instanceof DBPUniqueObject uo ? uo.getUniqueName() : o.getName();
    }
}
