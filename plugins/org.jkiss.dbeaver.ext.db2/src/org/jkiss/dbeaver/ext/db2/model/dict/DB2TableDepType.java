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
package org.jkiss.dbeaver.ext.db2.model.dict;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DB2 Type of Table Dependency
 *
 * @author Denis Forveille
 */
public enum DB2TableDepType implements DBPNamedObject {
    A("Table alias", DB2ObjectType.ALIAS),

    F("Routine", DB2ObjectType.ROUTINE),

    I("Index", DB2ObjectType.INDEX),

    G("Global temporary table", DB2ObjectType.TABLE),

    N("Nickname", DB2ObjectType.NICKNAME),

    O("Privilege dependency on all subtables or subviews in a table or view hierarchy"),

    R("UDT", DB2ObjectType.UDT),

    S("MQT", DB2ObjectType.MQT),

    T("Table", DB2ObjectType.TABLE),

    U("Typed table", DB2ObjectType.TABLE),

    V("View", DB2ObjectType.VIEW),

    W("Typed view", DB2ObjectType.VIEW),

    Z("XSR object", DB2ObjectType.XML_SCHEMA),

    m("Module", DB2ObjectType.MODULE),

    u("Module alias", DB2ObjectType.ALIAS),

    v("Global variable", DB2ObjectType.VARIABLE);

    private final String title;
    private final DB2ObjectType db2ObjectType;

    DB2TableDepType(String title, DB2ObjectType db2ObjectType) {
        this.title = title;
        this.db2ObjectType = db2ObjectType;
    }

    DB2TableDepType(String description) {
        this(description, null);
    }

    @NotNull
    @Override
    public String getName() {
        return title;
    }

    public DB2ObjectType getDb2ObjectType() {
        return db2ObjectType;
    }

}