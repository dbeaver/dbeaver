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
    REDSHIFT("Redshift", false, false, false, false, false, false, true),
    TIMESCALE("Timescale"),
    YELLOWBRICK("YellowBrick"),
    COCKROACH("Cockroach", true, true, false, false, false, false, false),
    OTHER("Postgre");

    private String name;
    private boolean supportsOids;
    private boolean supportsIndexes;
    private boolean supportsInheritance;
    private boolean supportsTriggers;
    private boolean supportsEncodings;
    private boolean supportsTablespaces;
    private boolean supportsLimits;

    PostgreServerType(String name) {
        this(name, true, true, true, true, true, true, true);
    }

    PostgreServerType(String name, boolean supportsOids, boolean supportsIndexes, boolean supportsInheritance, boolean supportsTriggers, boolean supportsEncodings, boolean supportsTablespaces, boolean supportsLimits) {
        this.name = name;
        this.supportsOids = supportsOids;
        this.supportsIndexes = supportsIndexes;
        this.supportsInheritance = supportsInheritance;
        this.supportsTriggers = supportsTriggers;
        this.supportsEncodings = supportsEncodings;
        this.supportsTablespaces = supportsTablespaces;
        this.supportsLimits = supportsLimits;
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

    public boolean isSupportsLimits() {
        return supportsLimits;
    }

    public boolean supportsEncodings() {
        return supportsEncodings;
    }

    public boolean isSupportsTablespaces() {
        return supportsTablespaces;
    }
}
