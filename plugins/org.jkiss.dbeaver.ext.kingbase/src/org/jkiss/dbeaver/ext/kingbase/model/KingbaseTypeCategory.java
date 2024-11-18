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
 * Enumerates system-defined values of typcategory.
 */
public enum KingbaseTypeCategory implements DBPNamedObject {

    A("Array"),
    B("Boolean"),
    C("Composite"),
    D("Date/time"),
    E("Enum"),
    G("Geometric"),
    I("Network address"),
    J("JSON"),
    M("Set"),
    N("Numeric"),
    P("Pseudo"),
    R("Range"), 
    S("String"),
    T("Timespan"),
    U("User-defined"),
    V("Bit-string"),
    X("Unknown"),
    Y("Collection"),
    Z("Internal-use types"); 

    private final String desc;

    KingbaseTypeCategory(String desc) {
        this.desc = desc;
    }

    @NotNull
    @Override
    public String getName() {
        return desc;
    }
}
