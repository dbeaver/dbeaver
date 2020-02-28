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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.model.DBPObject;

/**
 * Object class
 */
public enum SQLServerObjectClass implements DBPObject {
    DATABASE(0, "Database"),
    OBJECT_OR_COLUMN(1, "Object or column"),
    PARAMETER(2, "Parameter"),
    SCHEMA(3, "Schema"),
    DATABASE_PRINCIPAL(4, "Database principal"),
    ASSEMBLY(5, "Assembly"),
    TYPE(6, "Type"),
    INDEX(7, "Index"),
    XML_SCHEMA_COLLECTION(10, "XML schema collection"),
    MESSAGE_TYPE(15, "Message type"),
    SERVICE_CONTRACT(16, "Service contract"),
    SERVICE(17, "Service"),
    REMOTE_SERVICE_BINDING(18, "Remote service binding"),
    ROUTE(19, "Route"),
    DATASPACE(20, "Dataspace (filegroup or partition scheme)"),
    PARTITION_FUNCTION(21, "Partition function"),
    DATABASE_FILE(22, "Database file"),
    PLAN_GUIDE(27, "Plan guide");

    private final int classId;
    private final String className;

    SQLServerObjectClass(int classId, String className) {
        this.classId = classId;
        this.className = className;
    }

    public int getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }
}
