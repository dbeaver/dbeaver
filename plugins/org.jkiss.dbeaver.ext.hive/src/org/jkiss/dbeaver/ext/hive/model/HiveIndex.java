/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;


public class HiveIndex extends GenericTableIndex {
    private String indexType;
    private HiveTable indexTable;
    private String description;

    public HiveIndex(HiveTable table, String name, boolean persisted, String description, String indexType, HiveTable indexTable) {
        super(table, true, "", 0, name, DBSIndexType.OTHER, true);
        this.description = description;
        this.indexType = indexType;
        this.indexTable = indexTable;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 5)
    public String getDescription() {
        return description;
    }

    @Property(viewable = true, order = 6)
    public String getHiveIndexType() {
        return indexType;
    }

    @Property(viewable = true, order = 7)
    public HiveTable getIndexTable() {
        return indexTable;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getTable().getCatalog(),
                getTable().getSchema(),
                this);
    }

    @Property(viewable = false)
    @Override
    public String getQualifier() {
        return super.getQualifier();
    }

    @Property(viewable = false)
    @Override
    public long getCardinality() {
        return super.getCardinality();
    }

    @Property(viewable = false)
    @Override
    public DBSIndexType getIndexType() {
        return super.getIndexType();
    }
}
