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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * DataSourceStorage
 */
public class DataSourceMemoryStorage implements DBPDataSourceConfigurationStorage {
    private final byte[] data;

    public DataSourceMemoryStorage(@NotNull byte[] data) {
        this.data = data;
    }

    @NotNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    @Override
    public String getStorageId() {
        return "memory";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public String getStorageSubId() {
        return null;
    }
}
