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

    Map<String, Object> EMPTY_OPTIONS = Collections.unmodifiableMap(new HashMap<>());

    String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
        throws DBException;

}
