/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * JDBCDataType
 */
public class JDBCDataType<OWNER extends DBSObject> implements DBSDataType
{
    private final OWNER owner;
    private int valueType;
    private String name;
    private String remarks;
    private boolean isUnsigned;
    private boolean isSearchable;
    private int precision;
    private int minScale;
    private int maxScale;

    public JDBCDataType(
        OWNER owner,
        int valueType,
        String name,
        @Nullable String remarks,
        boolean unsigned,
        boolean searchable,
        int precision,
        int minScale,
        int maxScale)
    {
        this.owner = owner;
        this.valueType = valueType;
        this.name = name;
        this.remarks = remarks;
        isUnsigned = unsigned;
        isSearchable = searchable;
        this.precision = precision;
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    public JDBCDataType(OWNER owner, DBSTypedObject typed) {
        this(owner, typed.getTypeID(), typed.getTypeName(), null, false, false, typed.getPrecision(), typed.getScale(), typed.getScale());
    }

    @Override
    public String getTypeName()
    {
        return name;
    }

    @Override
    public String getFullTypeName() {
        return DBUtils.getFullTypeName(this);
    }

    @Override
    public int getTypeID()
    {
        return valueType;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }

    @NotNull
    @Override
    public OWNER getParentObject()
    {
        return owner;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return owner.getDataSource();
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return JDBCUtils.resolveDataKind(getDataSource(), name, valueType);
    }

    @Override
    public int getScale()
    {
        return minScale;
    }

    public boolean isUnsigned()
    {
        return isUnsigned;
    }

    public boolean isSearchable()
    {
        return isSearchable;
    }

    @Override
    public int getPrecision()
    {
        return precision;
    }

    @Override
    public long getMaxLength()
    {
        return precision;
    }

    @Nullable
    @Override
    public Object geTypeExtension() {
        return null;
    }

    @Nullable
    @Override
    public DBSDataType getComponentType(@NotNull DBRProgressMonitor monitor) throws DBCException {
        return null;
    }

    @Override
    public int getMinScale()
    {
        return minScale;
    }

    @Override
    public int getMaxScale()
    {
        return maxScale;
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
        return DBUtils.getDefaultOperators(attribute);
    }

    public String toString()
    {
        return owner.getName() + "." + name;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

}
