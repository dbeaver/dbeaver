/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    public DBDPseudoAttribute getPseudoAttribute() {
        return null;
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
