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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.model.DBPToolTipObject;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridShard implements DBSObject, DBPToolTipObject
{
    private CubridDataSource dataSource;
    private String shardType;
    private String shardVal;

    public CubridShard(CubridDataSource dataSource, String shardType, String shardVal) {
        this.dataSource = dataSource;
        this.shardType = shardType;
        this.shardVal = shardVal;
    }

    @NotNull
    @Override
    public String getName() {
        return getType();
    }
    
    @Property(viewable = true, updatable = true, listProvider = ShardTypeProvider.class, order = 1)
    public String getType() {
    	return shardType;
    }

    public void setType(String type) {
        this.shardType = type;
    }

    @Property(viewable = true, updatable = true, order = 2)
    public String getValue() {
        return shardVal;
    }

    public void setValue(String value) {
        this.shardVal = value;
    }

    @Override
    public String getObjectToolTip() {
        return getValue();
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @Override
    public CubridDataSource getDataSource() {
        return dataSource;
    }

    public static class ShardTypeProvider implements IPropertyValueListProvider<CubridShard> {

        @Override
        public boolean allowCustomValue() {
            return true;
        }

        @Nullable
        @Override
        public Object[] getPossibleValues(CubridShard object) {
            return CubridConstants.SHARD_TYPE;
        }
    }
}
