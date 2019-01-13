/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.util.List;

/**
 * Nested attribute binding
 */
public abstract class DBDAttributeBindingNested extends DBDAttributeBinding implements DBCAttributeMetaData {
    @NotNull
    protected final DBDAttributeBinding parent;

    protected DBDAttributeBindingNested(
        @NotNull DBDAttributeBinding parent,
        @NotNull DBDValueHandler valueHandler)
    {
        super(valueHandler);
        this.parent = parent;
    }

    @NotNull
    public DBPDataSource getDataSource() {
        return parent.getDataSource();
    }

    @Nullable
    public DBDAttributeBinding getParentObject() {
        return parent;
    }

    /**
     * Meta attribute (obtained from result set)
     */
    @NotNull
    public DBCAttributeMetaData getMetaAttribute() {
        return this;
    }

    @Override
    public boolean isReadOnly() {
        assert parent != null;
        return parent.getMetaAttribute().isReadOnly();
    }

    @Nullable
    @Override
    public DBCEntityMetaData getEntityMetaData() {
        assert parent != null;
        return parent.getMetaAttribute().getEntityMetaData();
    }

    /**
     * Row identifier (may be null)
     */
    @Nullable
    public DBDRowIdentifier getRowIdentifier() {
        assert parent != null;
        return parent.getRowIdentifier();
    }

    @Nullable
    @Override
    public List<DBSEntityReferrer> getReferrers() {
        return null;
    }


}
