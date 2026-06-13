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
package org.jkiss.dbeaver.ext.tibero;

public class TiberoConstants {

    public static final String DRIVER_CLASS_NAME = "com.tmax.tibero.jdbc.TbDriver";
    public static final int DEFAULT_PORT = 8629;
    public static final String DEFAULT_DATABASE = "tibero";
    public static final String DEFAULT_USER = "sys";

    public static final String PROP_SHOW_ONLY_ONE_SCHEMA = "tibero.show-only-one-schema";
    public static final String PROP_HIDE_EMPTY_SCHEMAS = "tibero.hide-empty-schemas";
    public static final String PROP_READ_COLUMN_COMMENTS = "tibero.read-column-comments";
    public static final String PROP_SHOW_SCHEMA_TABLE_DESCRIPTION = "tibero.show-schema-table-description";
    public static final String PROP_SHOW_SCHEMA_INDEX_TABLE_DESCRIPTION = "tibero.show-schema-index-table-description";
    public static final String PROP_SHOW_SCHEMA_TRIGGER_TABLE_DESCRIPTION = "tibero.show-schema-trigger-table-description";
    public static final String PROP_COMPILE_AFTER_SAVE = "tibero.compile-after-save";
    public static final String PROP_RECOMPILE_BODY_ON_SPEC_SAVE = "tibero.recompile-body-on-spec-save";

    public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";
    public static final String PROP_OBJECT_BODY_DEFINITION = "extendedDefinitionText";

    public static final String CMD_COMPILE = "org.jkiss.dbeaver.ext.tibero.code.compile";

    private TiberoConstants() {
    }
}
