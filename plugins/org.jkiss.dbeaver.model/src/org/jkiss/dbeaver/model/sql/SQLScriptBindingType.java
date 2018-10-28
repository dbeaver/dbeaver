/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql;

/**
 * Script-to-datasource binding type
 */
public enum SQLScriptBindingType {

        EXTERNAL("N/A", "External binding (IDE resources)"),
        ID("ID", "Connection unique ID"),
        URL("URL", "DataSource URL (jdbc:dbms://host:port/...)"),
        PARAMS("PARAMS", "DataSource parameters (name1=value1;name2=value2;...)");

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
    }
