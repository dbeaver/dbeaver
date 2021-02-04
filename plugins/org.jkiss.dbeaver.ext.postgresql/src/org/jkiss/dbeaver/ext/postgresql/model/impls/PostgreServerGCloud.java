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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;

/**
 * Google Cloud SQL - PostgreSQL.
 *
 * This driver will connect a Google Cloud SQL PostgreSQL instance using the
 * cloud-sql-jdbc-socket-factory provided by Google. This simplifies the connection
 * process, as it means that no client certificates and IP whitelisting is needed in
 * order to connect.
 * 
 * See https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory#postgresql-1
 * for more information.
 */
public class PostgreServerGCloud extends PostgreServerPostgreSQL {

    public static final String TYPE_ID = "gcloudpg";

    public PostgreServerGCloud(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String getServerTypeName() {
        return "Google Cloud SQL PostgreSQL";
    }
}
