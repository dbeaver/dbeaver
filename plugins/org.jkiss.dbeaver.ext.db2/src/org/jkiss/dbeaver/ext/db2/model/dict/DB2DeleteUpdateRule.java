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
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

/**
 * DB2 Foreign Key Delete/Update Rule
 * 
 * @author Denis Forveille
 */
public enum DB2DeleteUpdateRule implements DBPNamedObject {

    A("No Action", DBSForeignKeyModifyRule.NO_ACTION),

    C("Cascade", DBSForeignKeyModifyRule.CASCADE),

    N("Set Null", DBSForeignKeyModifyRule.SET_NULL),

    R("Restrict", DBSForeignKeyModifyRule.RESTRICT);

    private String name;
    private DBSForeignKeyModifyRule rule;

    // ------------
    // Constructors
    // ------------
    private DB2DeleteUpdateRule(String name, DBSForeignKeyModifyRule rule)
    {
        this.name = name;
        this.rule = rule;
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
    // Helper
    // ----------------
    public static DB2DeleteUpdateRule getDB2RuleFromDBSRule(DBSForeignKeyModifyRule dbsRule)
    {
        for (DB2DeleteUpdateRule db2Rule : DB2DeleteUpdateRule.values()) {
            if (db2Rule.getRule().equals(dbsRule)) {
                return db2Rule;
            }
        }
        return null;
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

    public DBSForeignKeyModifyRule getRule()
    {
        return rule;
    }
}