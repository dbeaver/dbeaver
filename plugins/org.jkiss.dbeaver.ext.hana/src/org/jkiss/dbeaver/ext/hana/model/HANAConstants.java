/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

public class HANAConstants {

	// boolean like columns in SYS schema are stored as VARCHAR(5) with values TRUE, FALSE
    public static final String SYS_BOOLEAN_TRUE = "TRUE";
    public static final String SYS_BOOLEAN_FALSE = "FALSE";
    
    // pseudo schema for PUBLIC SYNONYMs
    public static final String SCHEMA_PUBLIC = "PUBLIC";
}
