/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
 * DB2 Type of Package Dependency
 * 
 * @author Denis Forveille
 */
public enum DB2PackageDepType implements DBPNamedObject {
    A("Table alias", DB2ObjectType.ALIAS),

    B("Trigger", DB2ObjectType.TRIGGER),

    D("Server"),

    F("Routine", DB2ObjectType.ROUTINE),

    G("Global temporary table", DB2ObjectType.TABLE),

    I("Index", DB2ObjectType.INDEX),

    M("Function Mapping", DB2ObjectType.UDF),

    N("Nickname", DB2ObjectType.NICKNAME),

    O("Privilege dependency on all subtables or subviews in a table or view hierarchy"),

    P("Page Size"),

    Q("Sequence", DB2ObjectType.SEQUENCE),

    R("UDT", DB2ObjectType.UDT),

    S("MQT", DB2ObjectType.MQT),

    T("Table", DB2ObjectType.TABLE),

    U("Typed table", DB2ObjectType.TABLE),

    V("View", DB2ObjectType.VIEW),

    W("Typed view", DB2ObjectType.VIEW),

    X("Index extension"),

    Z("XSR object", DB2ObjectType.XML_SCHEMA),

    m("Module", DB2ObjectType.MODULE),

    n("Database Partiton Group"),

    q("Sequence alias", DB2ObjectType.ALIAS),

    u("Module alias", DB2ObjectType.ALIAS),

    v("Global variable", DB2ObjectType.VARIABLE),

    ZZ_4("Application-period temporal table"),

    ZZ_5("Application-period temporal table");

    public static final String FAKE_PREFIX = "ZZ_";

    private String name;
    private DB2ObjectType db2ObjectType;

    // -----------
    // Constructor
    // -----------

    private DB2PackageDepType(String name, DB2ObjectType db2ObjectType)
    {
        this.name = name;
        this.db2ObjectType = db2ObjectType;
    }

    private DB2PackageDepType(String name)
    {
        this(name, null);
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    public DB2ObjectType getDb2ObjectType()
    {
        return db2ObjectType;
    }

}