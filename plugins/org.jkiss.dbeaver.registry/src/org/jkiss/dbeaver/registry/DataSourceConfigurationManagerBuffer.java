/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class DataSourceConfigurationManagerBuffer implements DataSourceConfigurationManager {

    private byte[] data;

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public List<DBPDataSourceConfigurationStorage> getConfigurationStorages() {
        return null;
    }

    @Override
    public InputStream readConfiguration(@NotNull String name, Collection<String> dataSourceIds) throws DBException, IOException {
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void writeConfiguration(@NotNull String name, @Nullable byte[] data) throws DBException, IOException {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

}
