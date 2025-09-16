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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;

public class CubridView extends GenericView
{
    private CubridUser owner;
    public CubridView(
            @NotNull GenericStructContainer container,
            @Nullable String tableName,
            @Nullable String tableType,
            @Nullable JDBCResultSet dbResult) {
        super(container, tableName != null ? tableName.toLowerCase() : null, tableType, dbResult);
        if (dbResult != null) {
            String type = JDBCUtils.safeGetString(dbResult, CubridConstants.IS_SYSTEM_CLASS);
            if (type != null) {
                this.setSystem(type.equals("YES"));
            }
        }
        this.owner = (CubridUser) container;
    }

    @NotNull
    @Property(viewable = true, editable = true, order = 1)
    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void setName(String name) {
        super.setName(name != null ? name.toLowerCase() : null);
    }

    public void setSchema(@NotNull CubridUser owner) {
        this.owner = owner;
    }

    @Override
    @Property(viewable = true, order = 2)
    public String getTableType() {
        return super.getTableType();
    }

    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return (CubridDataSource) super.getDataSource();
    }

    @NotNull
    public String getUniqueName() {
        if (getDataSource().getSupportMultiSchema()) {
            return this.getContainer() + "." + this.getName();
        } else {
            return this.getName();
        }
    }

    public boolean isEnableSchema() {
        return getDataSource().getSupportMultiSchema() || getDataSource().isDBAGroup();
    }

    @NotNull
    @Override
    @Property(viewable = true, editableExpr = "object.enableSchema", updatableExpr = "object.enableSchema", listProvider = OwnerListProvider.class, labelProvider = GenericSchema.SchemaNameTermProvider.class, order = 2)
    public GenericSchema getSchema() {
        return owner;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return super.getDescription();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        if (this.isSystem() || !getDataSource().getSupportMultiSchema()) {
            return DBUtils.getFullQualifiedName(getDataSource(), this);
        } else {
            return DBUtils.getQuotedIdentifier(this.getSchema()) + "." + DBUtils.getFullQualifiedName(getDataSource(), this);
        }
    }

    public static class OwnerListProvider implements IPropertyValueListProvider<CubridView>
    {
        @NotNull
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @NotNull
        @Override
        public Object[] getPossibleValues(@NotNull CubridView object) {
            return object.getDataSource().getSchemas().toArray();
        }
    }
}
