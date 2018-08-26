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

package org.jkiss.dbeaver.ext.postgresql;

/**
 * Database type
 */
public enum PostgreServerType {

    POSTGRESQL("PostgreSQL"),
    GREENPLUM("Greenplum"),
    REDSHIFT("Redshift", false, false, false, false),
    TIMESCALE("Timescale"),
    YELLOWBRICK("YellowBrick"),
    OTHER("Postgre");

    private String name;
    private boolean supportsOids;
    private boolean supportsIndexes;
    private boolean supportsInheritance;
    private boolean supportsTriggers;

    PostgreServerType(String name) {
        this(name, true, true, true, true);
    }

    PostgreServerType(String name, boolean supportsOids, boolean supportsIndexes, boolean supportsInheritance, boolean supportsTriggers) {
        this.name = name;
        this.supportsOids = supportsOids;
        this.supportsIndexes = supportsIndexes;
        this.supportsInheritance = supportsInheritance;
        this.supportsTriggers = supportsTriggers;
    }

    public String getName() {
        return name;
    }

    public boolean supportsOids() {
        return supportsOids;
    }

    public boolean supportsIndexes() {
        return supportsIndexes;
    }

    public boolean supportsInheritance() {
        return supportsInheritance;
    }

    public boolean supportsTriggers() {
        return supportsTriggers;
    }
}
