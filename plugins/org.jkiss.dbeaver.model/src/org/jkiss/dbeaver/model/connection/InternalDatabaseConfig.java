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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;

public interface InternalDatabaseConfig {

    String getDriver();

    void setDriver(String driver);

    @NotNull
    String getUrl();

    void setUrl(String url);

    String getUser();

    String getPassword();

    String getSchema();

    Pool getPool();

    boolean isBackupEnabled();

    class Pool {
        private int minIdleConnections = 4;
        private int maxIdleConnections = 10;
        private int maxConnections = 100;
        private String validationQuery = "SELECT 1";

        public Pool() {
        }

        public Pool(
            int minIdleConnections,
            int maxIdleConnections,
            int maxConnections,
            String validationQuery
        ) {
            this.minIdleConnections = minIdleConnections;
            this.maxIdleConnections = maxIdleConnections;
            this.maxConnections = maxConnections;
            this.validationQuery = validationQuery;
        }

        public int getMinIdleConnections() {
            return minIdleConnections;
        }

        public int getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public String getValidationQuery() {
            return validationQuery;
        }
    }
}
