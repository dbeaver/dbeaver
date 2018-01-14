/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.debug.DBGObject;

@SuppressWarnings("nls")
public class PostgreDebugObject implements DBGObject {

    private final Integer oid;

    private final String proname;

    private final String owner;

    private final String schema;

    private final String lang;

    public PostgreDebugObject(Integer oid, String proname, String owner, String schema, String lang) {
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
