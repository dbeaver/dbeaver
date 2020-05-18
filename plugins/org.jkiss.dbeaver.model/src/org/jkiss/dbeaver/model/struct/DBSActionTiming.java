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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.Locale;

/**
 * DBSEntityConstraintType
 */
public class DBSActionTiming implements DBPNamedObject
{
    public static final DBSActionTiming BEFORE = new DBSActionTiming("BEFORE");
    public static final DBSActionTiming AFTER = new DBSActionTiming("AFTER");
    public static final DBSActionTiming INSTEAD = new DBSActionTiming("INSTEAD");
    public static final DBSActionTiming UNKNOWN = new DBSActionTiming("UNKNOWN");

    private final String name;

    protected DBSActionTiming(String name)
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

    public static DBSActionTiming getByName(String name)
    {
        if (name.toUpperCase(Locale.ENGLISH).equals(BEFORE.getName())) {
            return BEFORE;
        } else if (name.toUpperCase(Locale.ENGLISH).equals(AFTER.getName())) {
            return AFTER;
        } else {
            return UNKNOWN;
        }
    }
}