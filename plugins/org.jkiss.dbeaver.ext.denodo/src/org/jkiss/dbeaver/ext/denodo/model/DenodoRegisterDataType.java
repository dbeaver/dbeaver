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
package org.jkiss.dbeaver.ext.denodo.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

public class DenodoRegisterDataType extends DenodoDataType implements DBSEntity {

    private final AttributeCache attributeCache;

    public DenodoRegisterDataType(
        @NotNull DenodoDataSource owner,
        int valueType,
        @NotNull String name
    ) {
        super(owner, valueType, name, Kind.REGISTER);
        this.attributeCache = new AttributeCache(this);
    }

    public DenodoRegisterDataType(
        @NotNull DenodoArrayDataType ownerArrayType
    ) {
        super(ownerArrayType.getDataSource(), Types.STRUCT, "Item", Kind.REGISTER);
        this.attributeCache = new AttributeCache(this);
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TYPE;
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.attributeCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return this.attributeCache.getObject(monitor, this, attributeName);
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    private static class AttributeCache extends JDBCObjectCache<DenodoRegisterDataType, DenodoRegisterTypeAttribute> {

        private final DenodoDataType realOwnerType;

        public AttributeCache(@NotNull DenodoDataType realOwnerType) {
            this.realOwnerType = realOwnerType;
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
            @NotNull JDBCSession session, @NotNull DenodoRegisterDataType dataType
        ) throws SQLException {
            String sqlText = """
                SELECT type_name, type, attribute_name, attribute_vdp_type
                FROM GET_TYPE_ATTRIBUTES()
                WHERE type_name = ?
                """;
            JDBCPreparedStatement statement = session.prepareStatement(sqlText);
            statement.setString(1, realOwnerType.getName());
            return statement;
        }

        @NotNull
        @Override
        protected DenodoRegisterTypeAttribute fetchObject(
            @NotNull JDBCSession session,
            @NotNull DenodoRegisterDataType dataType,
            @NotNull JDBCResultSet resultSet
        ) throws SQLException, DBException {
            String attributeName = JDBCUtils.safeGetString(resultSet, "attribute_name");
            String denodoTypeName = JDBCUtils.safeGetString(resultSet, "attribute_vdp_type");
            assert denodoTypeName != null;
            DBSDataType attributeType = this.realOwnerType.getDataSource().resolveDataType(session.getProgressMonitor(), denodoTypeName);
            assert attributeType != null;
            assert attributeName != null;
            return new DenodoRegisterTypeAttribute(dataType, attributeType, attributeName, resultSet.getRow());
        }

        @Override
        protected void addCustomObjects(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DenodoRegisterDataType denodoRegisterDataType,
            @NotNull List<DenodoRegisterTypeAttribute> denodoRegisterTypeAttributes
        ) throws DBException {
            // resultSet.getRow() should be [1, 2, ..., rowsCount], but it actually is [1, 2, ..., rowsCount-1, 0]
            // so we can't just use resultSet.getRow()-1, and database metadata also doesn't have ordinal number,
            // but value handlers need it for data bindings, so reassign ordinal numbers manually
            for (int i = 0; i < denodoRegisterTypeAttributes.size(); i++) {
                denodoRegisterTypeAttributes.get(i).setOrdinalPosition(i);
            }
        }
    }

}