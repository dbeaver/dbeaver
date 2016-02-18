/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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