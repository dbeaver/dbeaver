/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;

import java.util.Collection;

/**
* PostgreDatabaseExportInfo
*/
public class PostgreDatabaseExportInfo {
    @NotNull
    private PostgreSchema schema;
    @Nullable
    private Collection<PostgreTableBase> tables;

    @NotNull
    public PostgreSchema getSchema() {
        return schema;
    }

    public PostgreDatabaseExportInfo(@NotNull PostgreSchema schema, @Nullable Collection<PostgreTableBase> tables) {
        this.schema = schema;
        this.tables = tables;
    }

    @Nullable
    public Collection<PostgreTableBase> getTables() {
        return tables;
    }

    @Override
    public String toString() {
        return schema.getName() + " " + tables;
    }
}
