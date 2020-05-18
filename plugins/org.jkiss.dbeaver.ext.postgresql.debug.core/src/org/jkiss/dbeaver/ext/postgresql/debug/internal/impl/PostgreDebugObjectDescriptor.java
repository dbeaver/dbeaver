/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.debug.internal.impl;

import org.jkiss.dbeaver.debug.DBGObjectDescriptor;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("nls")
public class PostgreDebugObjectDescriptor implements DBGObjectDescriptor {

    private final Integer oid;

    private final String proname;

    private final String owner;

    private final String schema;

    private final String lang;

    public PostgreDebugObjectDescriptor(Integer oid, String proname, String owner, String schema, String lang) {
        super();
        this.oid = oid;
        this.proname = proname;
        this.owner = owner;
        this.schema = schema;
        this.lang = lang;
    }

    public String getOwner() {
        return owner;
    }

    public String getSchema() {
        return schema;
    }

    public String getLang() {
        return lang;
    }

    @Override
    public Integer getID() {
        return oid;
    }

    @Override
    public String getName() {
        return proname;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("oid", oid);
        map.put("proname", proname);
        map.put("owner", owner);
        map.put("schema", schema);
        map.put("lang", lang);
        return map;
    }

    @Override
    public String toString() {
        return "id:" + String.valueOf(oid) + ", name: `" + proname + "(" + lang + ")" + ", user: " + owner + "("
                + schema + ")";
    }

}
