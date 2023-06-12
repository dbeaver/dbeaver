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
package org.jkiss.dbeaver.registry.storage;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface InternalDatabaseConfig {

    String getDriver();

    void setDriver(String driver);

    @NotNull
    String getUrl();

    void setUrl(String url);

    String getUser();

    String getPassword();

    /**
     * Sets prefix for sql queries params (f.e. schema names for tables)
     */
    @NotNull
    static String setPrefixes(@NotNull String sql, @Nullable String prefix) {
        return sql.replaceAll("\\{prefix}", prefix == null ? "" : prefix + ".");
    }
}
