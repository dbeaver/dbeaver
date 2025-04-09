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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolByDbObjectDefinition;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolClass;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolDefinition;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.struct.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents information about data type in the sql query semantic model
 */
public abstract class SQLQueryExprType {

    private static final Map<DBPDataKind, SQLQueryExprType>  PREDEFINED_TYPES = Arrays.stream(DBPDataKind.values()).collect(Collectors.toMap(
        k -> k,
        k -> new SQLQueryExprPredefinedType(k.name().toUpperCase(), k)
    ));

    public static final SQLQueryExprType UNKNOWN = forPredefined(DBPDataKind.UNKNOWN);
    public static final SQLQueryExprType STRING = forPredefined(DBPDataKind.STRING);
    public static final SQLQueryExprType BOOLEAN = forPredefined(DBPDataKind.BOOLEAN);
    public static final SQLQueryExprType NUMERIC = forPredefined(DBPDataKind.NUMERIC);
    public static final SQLQueryExprType DATETIME = forPredefined(DBPDataKind.DATETIME);

    public static final SQLQueryExprType DUMMY = new SQLQueryExprDummyType(null);
    private static final SQLQueryExprType DUMMY_FIELD = new SQLQueryExprDummyType(() -> SQLQuerySymbolClass.COMPOSITE_FIELD);

    protected final SQLQuerySymbolDefinition declaratorDefinition;
    protected final DBPDataKind dataKind;
    protected final DBSTypedObject typedObject;
    
    public SQLQueryExprType(@Nullable SQLQuerySymbolDefinition declaratorDefinition, @NotNull DBPDataKind dataKind) {
        this.declaratorDefinition = declaratorDefinition;
        this.dataKind = dataKind;
        this.typedObject = null;
    }
    
    public SQLQueryExprType(@Nullable SQLQuerySymbolDefinition declaratorDefinition, @NotNull DBSTypedObject typedObject) {
        this.declaratorDefinition = declaratorDefinition;
        this.dataKind = typedObject.getDataKind();
        this.typedObject = typedObject;
    }

    @Nullable
    public abstract String getDisplayName();

    @Nullable
    public final SQLQuerySymbolDefinition getDeclaratorDefinition() {
        return this.declaratorDefinition;
    }

    @NotNull
    public final DBPDataKind getDataKind() {
        return this.dataKind;
    }

    @Nullable
    public final DBSTypedObject getTypedDbObject() {
        return this.typedObject;
    }

    /**
     * Find a member with the specified name and return its type if exists
     */
    @Nullable
    public SQLQueryExprType findNamedMemberType(@NotNull DBRProgressMonitor monitor, @NotNull String memberName) throws DBException {
        return null;
    }

    public record SQLQueryExprTypeMemberInfo(
        @NotNull SQLQueryExprType declaratorType,
        @NotNull String name,
        @NotNull SQLQueryExprType type,
        @Nullable DBSEntityAttribute attribute,
        @Nullable SQLQueryResultColumn column) {
    }

    @NotNull
    public List<SQLQueryExprTypeMemberInfo> getNamedMembers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    /**
     * Find an indexed member by the specified indexes and return corresponding item type if exists
     */
    @Nullable
    public SQLQueryExprType findIndexedItemType(
        @NotNull DBRProgressMonitor monitor,
        int depth,
        @Nullable boolean[] slicingSpec
    ) throws DBException {
        return null;
    }

    @NotNull
    public static SQLQueryExprType forPredefined(DBPDataKind dataKind) {
        SQLQueryExprType result = PREDEFINED_TYPES.get(dataKind);
        assert result != null;
        return result;
    }

    /**
     * Prepare type info based on the scalar subquery result
     */
    @NotNull
    public static SQLQueryExprType forScalarSubquery(@NotNull SQLQueryRowsSourceModel source) {
        List<SQLQueryResultColumn> columns = source.getResultDataContext().getColumnsList();
        return columns.isEmpty() ? SQLQueryExprType.UNKNOWN : columns.get(0).type;
    }

    /**
     * Prepare predefined type info based on the type name
     */
    @NotNull
    public static SQLQueryExprType forExplicitTypeRef(@NotNull String typeRefString) {
        return new SQLQueryExprPredefinedType(typeRefString, DBPDataKind.UNKNOWN);
    }

    public static SQLQueryExprType forReferencedRow(SQLQuerySymbolEntry reference, SourceResolutionResult referencedSource) {
        return new SQLQueryExprRowType(reference, referencedSource);
    }

    /**
     * Prepare type info based on the metadata
     */
    @NotNull
    public static SQLQueryExprType forTypedObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSTypedObject typedObj,
        @NotNull SQLQuerySymbolClass defSymbolClass
    ) throws DBException {
        return forTypedObjectImpl(
            monitor, typedObj,
            typedObj instanceof DBSObject dbObj ? new SQLQuerySymbolByDbObjectDefinition(dbObj, defSymbolClass) : null
        );
    }

    @NotNull
    private static SQLQueryExprType forTypedObjectImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSTypedObject typedObj,
        @Nullable SQLQuerySymbolDefinition declaratorDefinition
    ) throws DBException {
        if (SQLQueryDummyDataSourceContext.isDummyObject(typedObj)) {
            return DUMMY;
        }
        
        if (typedObj instanceof DBSTypedObjectEx2 typedEx2) {
            SQLQueryExprType type = forDescribedIfPresented(monitor, typedObj, typedEx2.getTypeDescriptor(monitor), declaratorDefinition);
            if (type != null) {
                return type;
            }
        }
        
        if (typedObj instanceof DBSTypedObjectEx typedEx) {
            DBSDataType type = typedEx.getDataType();
            if (type != null) {
                return forTypedObjectImpl(monitor, type, declaratorDefinition);
            }
        }

        if (typedObj instanceof DBSDataType type) {
            DBSDataType itemType = type.getComponentType(monitor);
            if (itemType != null) {
                return new SQLQueryExprIndexableType(declaratorDefinition, typedObj, itemType);
            } else if (typedObj instanceof DBSEntity complexType) {
                DBPDataSource dataSource = complexType.getDataSource();
                SQLDialect dialect = dataSource == null ? BasicSQLDialect.INSTANCE : dataSource.getSQLDialect();
                List<? extends DBSEntityAttribute> attrs = complexType.getAttributes(monitor);
                if (attrs != null) {
                    Map<String, DBSEntityAttribute> attrsByName = attrs.stream().collect(Collectors.toMap(
                        a -> SQLUtils.identifierToCanonicalForm(dialect, a.getName(), false, true),
                        a -> a,
                        (a, b) -> a)
                    );
                    return new SQLQueryExprComplexType<>(declaratorDefinition, (DBSEntity & DBSTypedObject) complexType, attrsByName);
                }
            }
        }
        
        return new SQLQueryExprSimpleType(declaratorDefinition, typedObj);
    }

    @Nullable
    private static SQLQueryExprType forDescribedIfPresented(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSTypedObject typedObj,
        @Nullable DBSTypeDescriptor typeDesc,
        @Nullable SQLQuerySymbolDefinition declaratorDefinition
    ) throws DBException {
        if (typeDesc == null) {
            return null;
        }
        
        if (typeDesc.isIndexable()) {
            return new SQLQueryExprDescribedIndexableType(declaratorDefinition, typedObj, typeDesc);
        } else if (typeDesc.getUnderlyingType() != null) {
            return forTypedObjectImpl(monitor, typeDesc.getUnderlyingType(), declaratorDefinition);
        }
            
        return null;
    }

    @Nullable
    public static SQLQueryExprType tryCombineIfMatches(@NotNull SQLQueryExprType a, @NotNull SQLQueryExprType b) {
        if (a.getDataKind().equals(DBPDataKind.ANY)) {
            return a;
        }
        if (b.getDataKind().equals(DBPDataKind.ANY)) {
            return b;
        }
        
        boolean matches = (
                // both are simple of the same kind
                !a.getDataKind().isComplex() &&
                !b.getDataKind().isComplex() &&
                a.getDataKind().equals(b.getDataKind())
            ) || (
                // both are complex of the exact same db type
                a instanceof SQLQueryExprComplexType x && 
                b instanceof SQLQueryExprComplexType y && 
                x.complexType.equals(y.complexType)
            ) || (
                a instanceof SQLQueryExprIndexableType x &&
                b instanceof SQLQueryExprIndexableType y &&
                isDataTypeMatches(x.elementType, y.elementType)
            ) || (
                a instanceof SQLQueryExprDescribedIndexableType x &&
                b instanceof SQLQueryExprDescribedIndexableType y &&
                x.typeDesc.equals(y.typeDesc)
            );
        
        // TODO consider dialect-dependent coercions, consider generalizing coercion 
        return matches ? a : null;
    }
    
    private static boolean isDataTypeMatches(@NotNull DBSDataType a, @NotNull DBSDataType b) {
        return (
                // both are simple of the same kind
                !a.getDataKind().isComplex() &&
                !b.getDataKind().isComplex() &&
                a.getDataKind().equals(b.getDataKind())
            ) || (
                // both are complex of the exact same db type
                a.getDataKind().isComplex() &&
                b.getDataKind().isComplex() &&
                a.equals(b)
            );
    }

    private static class SQLQueryExprRowType extends SQLQueryExprType {
        private final SQLQuerySymbolEntry reference;
        private final SourceResolutionResult referencedSource;

        public SQLQueryExprRowType(SQLQuerySymbolEntry reference, SourceResolutionResult referencedSource) {
            super(reference.getDefinition(), DBPDataKind.ANY);
            this.reference = reference;
            this.referencedSource = referencedSource;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.referencedSource.tableOrNull != null ? DBUtils.getObjectTypeName(this.referencedSource.tableOrNull) : this.reference.getName();
        }

        @Override
        public @NotNull List<SQLQueryExprTypeMemberInfo> getNamedMembers(@NotNull DBRProgressMonitor monitor) throws DBException {
            return this.referencedSource.source.getResultDataContext().getColumnsList().stream().map(
                c -> new SQLQueryExprTypeMemberInfo(this, c.symbol.getName(), c.type, c.realAttr, c)
            ).toList();
        }

        @Override
        public SQLQueryExprType findNamedMemberType(@NotNull DBRProgressMonitor monitor, @NotNull String memberName) throws DBException {
            SQLQueryResultColumn column = this.referencedSource.source.getResultDataContext().resolveColumn(monitor, memberName);
            return column == null ? null : column.type;
        }

        @Override
        public String toString() {
            return "RowType[" + this.getDisplayName() + "]";
        }
    }

    private static class SQLQueryExprComplexType<T extends DBSEntity & DBSTypedObject> extends SQLQueryExprType {
        private final T complexType;
        private final Map<String, DBSEntityAttribute> attrs;

        public SQLQueryExprComplexType(
            @Nullable SQLQuerySymbolDefinition declaratorDefinition,
            @NotNull T complexType,
            @NotNull Map<String, DBSEntityAttribute> attrs
        ) {
            super(declaratorDefinition, complexType);
            this.complexType = complexType;
            this.attrs = attrs;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.complexType.getFullTypeName();
        }

        @Override
        public @NotNull List<SQLQueryExprTypeMemberInfo> getNamedMembers(@NotNull DBRProgressMonitor monitor) throws DBException {
            if (attrs != null) {
                List<SQLQueryExprTypeMemberInfo> result = new ArrayList<>(attrs.size());
                for (DBSEntityAttribute attr : this.attrs.values()) {
                    result.add(new SQLQueryExprTypeMemberInfo(this, attr.getName(), forTypedObject(monitor, attr, SQLQuerySymbolClass.COMPOSITE_FIELD), attr, null));
                }
                return result;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public SQLQueryExprType findNamedMemberType(@NotNull DBRProgressMonitor monitor, @NotNull String memberName) throws DBException {
            DBSAttributeBase attr = attrs.get(memberName);
            SQLDialect dialect = this.complexType.getDataSource().getSQLDialect();
            if (attr == null) {
                String unquoted = dialect.getUnquotedIdentifier(memberName);
                attr = this.complexType.getAttribute(monitor, unquoted);
                if (attr == null) {
                    // "some" database plugins "intentionally" doesn't implement data type's attribute lookup, so try to mimic its logic
                    boolean isQuoted = DBUtils.isQuotedIdentifier(this.complexType.getDataSource(), memberName);
                    if ((!isQuoted && dialect.storesUnquotedCase() == DBPIdentifierCase.MIXED) || dialect.useCaseInsensitiveNameLookup()) {
                        attr = attrs.entrySet().stream()
                            .filter(e -> e.getKey().equalsIgnoreCase(unquoted))
                            .findFirst().map(Map.Entry::getValue).orElse(null);
                    }
                }
            }
            return attr == null ? null : forTypedObject(monitor, attr, SQLQuerySymbolClass.COMPOSITE_FIELD);
        }
        
        @Override
        public String toString() {
            return "ComplexType[" + this.complexType.getFullTypeName() + "]";
        }
    }
    
    private static class SQLQueryExprIndexableType extends SQLQueryExprType {
        private final DBSDataType elementType;
        
        public SQLQueryExprIndexableType(
            @Nullable SQLQuerySymbolDefinition declaratorDefinition,
            @NotNull DBSTypedObject typedObject,
            @NotNull DBSDataType elementType
        ) {
            super(declaratorDefinition, typedObject);
            this.elementType = elementType;
        }
        
        @Override
        public String getDisplayName() {
            return this.typedObject.getFullTypeName();
        }
        
        private SQLQueryExprType prepareElementType(DBRProgressMonitor monitor) throws DBException {
            return SQLQueryExprType.forTypedObjectImpl(monitor, this.elementType, this.getDeclaratorDefinition());
        }
        
        @Override
        public SQLQueryExprType findIndexedItemType(
            @NotNull DBRProgressMonitor monitor,
            int depth,
            @Nullable boolean[] slicingSpec
        ) throws DBException {
            if (slicingSpec == null) { // TODO take a look at the SQL Standard
                SQLQueryExprType type = this.prepareElementType(monitor);
                return depth == 1 ? type : type.findIndexedItemType(monitor, depth - 1, slicingSpec);
            } else {
                return slicingSpec[slicingSpec.length - depth] ? this : this.prepareElementType(monitor);
            }
        }

        @NotNull
        @Override
        public String toString() {
            return "IndexableType[" + this.elementType.getFullTypeName() + "]";
        }
    }
    
    private static class SQLQueryExprDescribedIndexableType extends SQLQueryExprType {
        private final DBSTypeDescriptor typeDesc;
        
        public SQLQueryExprDescribedIndexableType(
            @Nullable SQLQuerySymbolDefinition declaratorDefinition,
            @NotNull DBSTypedObject typedObject,
            @NotNull DBSTypeDescriptor typeDesc
        ) {
            super(declaratorDefinition, typedObject);
            this.typeDesc = typeDesc;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.typeDesc.getTypeName();
        }

        @Nullable
        @Override
        public SQLQueryExprType findIndexedItemType(
            @NotNull DBRProgressMonitor monitor,
            int depth,
            @Nullable boolean[] slicingSpec
        ) throws DBException {
            return SQLQueryExprType.forDescribedIfPresented(
                monitor,
                this.typedObject,
                this.typeDesc.getIndexableItemType(depth, slicingSpec),
                this.getDeclaratorDefinition()
            );
        }

        @NotNull
        @Override
        public String toString() {
            return "DescribedIndexableType[" + this.typeDesc.getTypeName() + "]";
        }
    }
    
    private static class SQLQueryExprSimpleType extends SQLQueryExprType {
        
        public SQLQueryExprSimpleType(@Nullable SQLQuerySymbolDefinition declaratorDefinition, @NotNull DBSTypedObject typedObject) {
            super(declaratorDefinition, typedObject);
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.typedObject.getFullTypeName();
        }

        @NotNull
        @Override
        public String toString() {
            return "SimpleType[" + this.typedObject.getFullTypeName() + "]";
        }
    }
    
    private static class SQLQueryExprPredefinedType extends SQLQueryExprType {
        private final String name;
        
        public SQLQueryExprPredefinedType(@NotNull String name, @NotNull DBPDataKind kind) {
            super(null, kind);
            this.name = name;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return this.name;
        }

        @NotNull
        @Override
        public String toString() {
            return "PredefinedType[" + this.name + "]";
        }
    }

    private static class SQLQueryExprDummyType extends SQLQueryExprType {

        public SQLQueryExprDummyType(@Nullable SQLQuerySymbolDefinition declaratorDefinition) {
            super(declaratorDefinition, DBPDataKind.ANY);
        }

        @Nullable
        @Override
        public String getDisplayName() {
            return null;
        }

        @NotNull
        @Override
        public SQLQueryExprType findNamedMemberType(@NotNull DBRProgressMonitor monitor, @NotNull String memberName) {
            return DUMMY_FIELD;
        }

        @NotNull
        @Override
        public SQLQueryExprType findIndexedItemType(@NotNull DBRProgressMonitor monitor, int depth, @Nullable boolean[] slicingSpec) {
            return DUMMY_FIELD;
        }

        @NotNull
        @Override
        public String toString() {
            return "DummyType[]";
        }
    }
}
