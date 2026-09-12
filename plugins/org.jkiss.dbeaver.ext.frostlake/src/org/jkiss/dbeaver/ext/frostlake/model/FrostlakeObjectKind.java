/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;

/**
 * The Frostlake object kinds that have no JDBC metadata call, each named by the SHOW command that
 * lists it.
 *
 * <p>Two scopes: most hang off a schema, while warehouses, roles and users are account-wide.
 *
 * <p>Sequences are absent deliberately — JDBC metadata already lists them, so the generic model serves
 * that folder and a SHOW here would only duplicate it.
 *
 * <p>They share one model class rather than getting one apiece because every Frostlake SHOW listing
 * carries the same identifying columns — {@code name}, {@code created_on}, {@code comment} and
 * {@code schema_name} — regardless of how many columns follow (9 for FILE FORMATS, 38 for WAREHOUSES).
 * The extra columns are surfaced as properties without this enum needing to know about them, so adding
 * a kind is one line here and one folder in plugin.xml.
 */

public enum FrostlakeObjectKind {

    // Schema-level: listed with IN SCHEMA.
    STAGE("STAGES", "stage", true),
    PIPE("PIPES", "pipe", true),
    STREAM("STREAMS", "stream", true),
    TASK("TASKS", "task", true),
    FILE_FORMAT("FILE FORMATS", "fileFormat", true),
    DYNAMIC_TABLE("DYNAMIC TABLES", "dynamicTable", true),
    TAG("TAGS", "tag", true),
    MASKING_POLICY("MASKING POLICIES", "maskingPolicy", true),
    ROW_ACCESS_POLICY("ROW ACCESS POLICIES", "rowAccessPolicy", true),
    CORTEX_SEARCH_SERVICE("CORTEX SEARCH SERVICES", "cortexSearchService", true),

    // Account-level: they belong to no schema, and IN SCHEMA is a syntax error for them.
    WAREHOUSE("WAREHOUSES", "warehouse", false),
    ROLE("ROLES", "role", false),
    USER("USERS", "user", false);

    private final String showKeyword;
    private final String nodePath;
    private final boolean schemaScoped;

    FrostlakeObjectKind(@NotNull String showKeyword, @NotNull String nodePath, boolean schemaScoped) {
        this.showKeyword = showKeyword;
        this.nodePath = nodePath;
        this.schemaScoped = schemaScoped;
    }

    /**
     * Whether this kind lives in a schema. Warehouses, roles and users do not — they are account-wide,
     * {@code IN SCHEMA} is a syntax error for them, and their folders hang off the data source rather
     * than off a schema.
     */
    public boolean isSchemaScoped() {
        return schemaScoped;
    }

    /** The plural that follows SHOW, e.g. {@code FILE FORMATS}. */
    @NotNull
    public String getShowKeyword() {
        return showKeyword;
    }

    /** The node path used for this kind in plugin.xml, and so in navigator URLs. */
    @NotNull
    public String getNodePath() {
        return nodePath;
    }

    /**
     * The listing statement for one schema, qualified with the catalog so the answer does not depend on
     * which database the session happens to be pointed at. The caller passes the schema reference
     * already qualified and quoted — an identifier that was created quoted and lower-case only survives
     * the round trip if it is quoted on the way back in, since Frostlake folds an unquoted one to
     * upper-case exactly as Snowflake does.
     */
    @NotNull
    public String getSchemaListQuery(@NotNull String qualifiedSchemaName) {
        return "SHOW " + showKeyword + " IN SCHEMA " + qualifiedSchemaName;
    }

    /** The listing statement for an account-level kind, which takes no scope at all. */
    @NotNull
    public String getAccountListQuery() {
        return "SHOW " + showKeyword;
    }
}
