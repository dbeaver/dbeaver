/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

    // HANA cloud connections use regular HTTPS port
    public static final String HTTPS_PORT_SUFFIX = ":443";
    
	// boolean like columns in SYS schema are stored as VARCHAR(5) with values TRUE, FALSE
    public static final String SYS_BOOLEAN_TRUE = "TRUE";
    public static final String SYS_BOOLEAN_FALSE = "FALSE";
    
    // pseudo schema for PUBLIC SYNONYMs
    public static final String SCHEMA_PUBLIC = "PUBLIC";

    // Data type names
    public static final String DATA_TYPE_NAME_REAL_VECTOR = "REAL_VECTOR";
    public static final String DATA_TYPE_NAME_ST_GEOMETRY = "ST_GEOMETRY";
    public static final String DATA_TYPE_NAME_ST_POINT = "ST_POINT";

    // connection properties
    public static final String CONN_PROP_APPLICATION_NAME = "SESSIONVARIABLE:APPLICATION";
    public static final String CONN_PROP_READONLY = "READONLY";
    public static final String CONN_PROP_SPATIAL_OUTPUT_REPRESENTATION = "SESSIONVARIABLE:SPATIAL_OUTPUT_REPRESENTATION";
    public static final String CONN_VALUE_SPATIAL_OUTPUT_REPRESENTATION = "EWKB";
    public static final String CONN_PROP_SPATIAL_WKB_EMPTY_POINT_REPRESENTATION = "SESSIONVARIABLE:SPATIAL_WKB_EMPTY_POINT_REPRESENTATION";
    public static final String CONN_VALUE_SPATIAL_WKB_EMPTY_POINT_REPRESENTATION = "NAN_COORDINATES";   
    
    // error codes, see SYS.M_ERROR_CODES view
    public static final int ERR_SQL_ALTER_PASSWORD_NEEDED = 414;
    public static final int WRN_SQL_NEARLY_EXPIRED_PASSWORD = 431;
}
