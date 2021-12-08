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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class SQLServerExternalTable extends SQLServerTable implements DBPImageProvider {
    private final String externalDataSource;
    private final String externalFileFormat;
    private final String externalLocation;

    public SQLServerExternalTable(@NotNull SQLServerSchema catalog, @NotNull ResultSet dbResult) {
        super(catalog, dbResult);
        this.externalDataSource = JDBCUtils.safeGetString(dbResult, "data_source_name");
        this.externalFileFormat = JDBCUtils.safeGetString(dbResult, "file_format_name");
        this.externalLocation = JDBCUtils.safeGetString(dbResult, "location");
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getExternalDataSource() {
        return externalDataSource;
    }

    @Nullable
    @Property(viewable = true, order = 8)
    public String getExternalFileFormat() {
        return externalFileFormat;
    }

    @NotNull
    @Property(viewable = true, order = 9)
    public String getExternalLocation() {
        return externalLocation;
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBIcon.TREE_TABLE_EXTERNAL;
    }
}
