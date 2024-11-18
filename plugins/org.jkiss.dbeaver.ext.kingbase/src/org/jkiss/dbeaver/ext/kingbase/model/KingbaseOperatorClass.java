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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * KingbaseOperatorClass
 */
public class KingbaseOperatorClass extends KingbaseInformation {

    private long oid;
    private final KingbaseAccessMethod accessMethod;
    private String name;
    private long namespaceId;
    private long ownerId;
    private long familyId;
    private long typeId;
    private boolean isDefault;
    private long keyTypeId;

    public KingbaseOperatorClass(KingbaseAccessMethod accessMethod, ResultSet dbResult)
        throws SQLException
    {
        super(accessMethod.getDatabase());
        this.accessMethod = accessMethod;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "opcname");
        this.namespaceId = JDBCUtils.safeGetLong(dbResult, "opcnamespace");
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "opcowner");
        this.familyId = JDBCUtils.safeGetLong(dbResult, "opcfamily");
        this.typeId = JDBCUtils.safeGetLong(dbResult, "opcintype");
        this.isDefault = JDBCUtils.safeGetBoolean(dbResult, "opcdefault");
        this.keyTypeId = JDBCUtils.safeGetLong(dbResult, "opckeytype");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 3)
    public KingbaseSchema getNamespace(DBRProgressMonitor monitor) throws DBException {
        return accessMethod.getDatabase().getSchema(monitor, namespaceId);
    }

    @Property(viewable = true, order = 4)
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return accessMethod.getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(viewable = true, order = 5, visibleIf = KingbaseSupportsOpFamily.class)
    public KingbaseOperatorFamily getFamily(DBRProgressMonitor monitor) throws DBException {
        return accessMethod.getOperatorFamily(monitor, familyId);
    }

    @Property(viewable = true, order = 6)
    public KingbaseDataType getType(DBRProgressMonitor monitor) {
        return accessMethod.getDatabase().getDataType(monitor, typeId);
    }

    @Property(viewable = true, order = 7)
    public boolean isDefault() {
        return isDefault;
    }

    @Property(viewable = true, order = 8)
    public KingbaseDataType getKeyType(DBRProgressMonitor monitor) {
        if (keyTypeId == 0) {
            return getType(monitor);
        }
        return accessMethod.getDatabase().getDataType(monitor, keyTypeId);
    }

    public static class KingbaseSupportsOpFamily implements IPropertyValueValidator<KingbaseOperatorClass, Object> {
        @Override
        public boolean isValidValue(KingbaseOperatorClass object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsOpFamily();
        }
    }

}

