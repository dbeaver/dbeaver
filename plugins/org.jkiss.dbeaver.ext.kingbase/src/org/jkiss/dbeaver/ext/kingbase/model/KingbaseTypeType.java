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
package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * KingbaseTypeType
 */
public enum KingbaseTypeType implements DBPNamedObject
{
    a("Associative_varray"), 
    b("Base"),
    c("Composite"),
    d("Domain"),
    e("Enum type"),
    m("Multirange"), 
    n("Nested_table"),
    o("Record"),
    p("Pseudo-type"),
    r("Range"),
    s("Set"),
    u("Unknown-type"),
    v("Varray-type");

    private final String desc;

    KingbaseTypeType(String desc) {
        this.desc = desc;
    }

    @NotNull
    @Override
    public String getName() {
        return desc;
    }
}
