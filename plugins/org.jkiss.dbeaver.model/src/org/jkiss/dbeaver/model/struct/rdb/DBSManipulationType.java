/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.Locale;

/**
 * DBSManipulationType
 */
public class DBSManipulationType implements DBPNamedObject
{
    public static final DBSManipulationType INSERT = new DBSManipulationType("INSERT");
    public static final DBSManipulationType DELETE = new DBSManipulationType("DELETE");
    public static final DBSManipulationType UPDATE = new DBSManipulationType("UPDATE");
    public static final DBSManipulationType TRUNCATE = new DBSManipulationType("TRUNCATE");
    public static final DBSManipulationType UNKNOWN = new DBSManipulationType("UNKNOWN");

    private final String name;

    protected DBSManipulationType(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString()
    {
        return getName();
    }

    public static DBSManipulationType getByName(String name)
    {
        if (name.toUpperCase(Locale.ENGLISH).equals(INSERT.getName())) {
            return INSERT;
        } else if (name.toUpperCase(Locale.ENGLISH).equals(DELETE.getName())) {
            return DELETE;
        } if (name.toUpperCase(Locale.ENGLISH).equals(UPDATE.getName())) {
            return UPDATE;
        } else {
            return UNKNOWN;
        }
    }
}