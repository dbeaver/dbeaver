/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.dict;

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