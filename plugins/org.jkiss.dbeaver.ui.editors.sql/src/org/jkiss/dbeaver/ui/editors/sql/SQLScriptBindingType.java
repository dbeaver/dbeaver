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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceUtils;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Script-to-datasource binding type
 */
public enum SQLScriptBindingType {

    EXTERNAL("N/A", "External binding (IDE resources)") {
        @Override
        public void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec) {
            // do nothing
        }
    },
    ID("ID", "Connection unique ID") {
        @Override
        public void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec) {
            spec.append(DataSourceUtils.PARAM_ID).append("=").append(dataSource.getId());
        }
    },
    NAME("NAME", "Connection name") {
        @Override
        public void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec) {
            spec.append(DataSourceUtils.PARAM_NAME).append("=").append(dataSource.getName());
        }
    },
    URL("URL", "Connection URL (jdbc:dbms://host:port/...)") {
        @Override
        public void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec) {
            spec.append(DataSourceUtils.PARAM_URL).append("=").append(dataSource.getConnectionConfiguration().getUrl());
        }
    },
    PARAMS("PARAMS", "Connection parameters (name1=value1;name2=value2;...)") {
        @Override
        public void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec) {
            DBPConnectionConfiguration cfg = dataSource.getConnectionConfiguration();
            Map<String,String> params = new LinkedHashMap<>();
            if (!CommonUtils.isEmpty(cfg.getServerName())) {
                params.put(DataSourceUtils.PARAM_SERVER, cfg.getServerName());
            }
            if (!CommonUtils.isEmpty(cfg.getHostName())) {
                params.put(DataSourceUtils.PARAM_HOST, cfg.getHostName());
            }
            if (!CommonUtils.isEmpty(cfg.getHostPort())) {
                params.put(DataSourceUtils.PARAM_PORT, cfg.getHostPort());
            }
            if (!CommonUtils.isEmpty(cfg.getDatabaseName())) {
                params.put(DataSourceUtils.PARAM_DATABASE, cfg.getDatabaseName());
            }
            if (!CommonUtils.isEmpty(cfg.getUserName())) {
                params.put(DataSourceUtils.PARAM_USER, cfg.getUserName());
            }
            for (DBWHandlerConfiguration handler : cfg.getDeclaredHandlers()) {
                if (!handler.isEnabled()) {
                    continue;
                }
                for (Map.Entry<String, String> prop : handler.getProperties().entrySet()) {
                    String propName = prop.getKey();
                    if (propName.contains(DataSourceUtils.PARAM_SERVER) || propName.contains(DataSourceUtils.PARAM_HOST) || propName.contains(DataSourceUtils.PARAM_PORT)) {
                        params.put("handler." + handler.getId() + "." + propName, prop.getValue());
                    }
                }
            }
            boolean first = true;
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (!first) spec.append("|");
                spec.append(param.getKey()).append("=").append(param.getValue());
                first = false;
            }
        }
    };

    private final String name;
    private final String description;

    SQLScriptBindingType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract void appendSpec(DBPDataSourceContainer dataSource, StringBuilder spec);

}
