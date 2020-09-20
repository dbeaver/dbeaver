/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Object with DDL
 */
public interface DBPScriptObject extends DBPObject {

    // If object definition was cached - refresh it
    String OPTION_REFRESH = "refresh";
    // Use fully qualified names. True by default
    String OPTION_FULLY_QUALIFIED_NAMES = "useFQN";
    String OPTION_INCLUDE_OBJECT_DROP = "script.includeDrop";
    String OPTION_SCRIPT_FORMAT = "script.format";
    String OPTION_SCRIPT_FORMAT_COMPACT = "script.format.compact";

    String OPTION_DDL_SOURCE = "ddl.source";

    // Extracts object source for debugger
    // By defautl the same as regular source but in some cases source should be transormed (e.g. for PG)
    String OPTION_DEBUGGER_SOURCE = "debugger.source";

    // Embedded source is used for obtaining source of
    // nested objects (columns, constraints, etc) which can be embedded in parent object declaration (tables)
    String OPTION_EMBEDDED_SOURCE = "embedded.source";

    // Means that result script will be used for object save
    String OPTION_OBJECT_SAVE = "object.save";

    // Can be used by DDL generators to generate object ALTER but CREATE.
    String OPTION_OBJECT_ALTER = "object.alter";

    String OPTION_DDL_SKIP_FOREIGN_KEYS = "ddl.skipForeignKeys"; //$NON-NLS-1$
    String OPTION_DDL_ONLY_FOREIGN_KEYS = "ddl.onlyForeignKeys"; //$NON-NLS-1$

    String OPTION_INCLUDE_NESTED_OBJECTS = "ddl.includeNestedObjects"; //$NON-NLS-1$
    String OPTION_INCLUDE_COMMENTS = "ddl.includeComments"; //$NON-NLS-1$
    String OPTION_INCLUDE_PERMISSIONS = "ddl.includePermissions"; //$NON-NLS-1$

    String OPTION_USE_SPECIAL_NAME = "ddl.useSpecialName"; //$NON-NLS-1$

    Map<String, Object> EMPTY_OPTIONS = Collections.unmodifiableMap(new HashMap<>());

    String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
        throws DBException;

}
