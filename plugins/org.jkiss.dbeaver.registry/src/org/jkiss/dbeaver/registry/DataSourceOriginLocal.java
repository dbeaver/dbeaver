/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.DBPImage;

import java.util.Collections;
import java.util.Map;

/**
 * DataSourceOriginProviderLocal
 */
public class DataSourceOriginLocal implements DBPDataSourceOrigin
{
    public static final String ORIGIN_ID = "local";

    public static final DBPDataSourceOrigin INSTANCE = new DataSourceOriginLocal();

    private DataSourceOriginLocal() {
    }

    @NotNull
    @Override
    public String getId() {
        return ORIGIN_ID;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Local";
    }

    @Nullable
    @Override
    public DBPImage getIcon() {
        return DBIcon.TREE_FILE;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @NotNull
    @Override
    public Map<String, Object> getConfiguration() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return getId();
    }

}
