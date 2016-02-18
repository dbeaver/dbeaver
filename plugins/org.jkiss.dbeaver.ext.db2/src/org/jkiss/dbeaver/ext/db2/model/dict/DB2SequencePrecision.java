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

/**
 * DB2 "Precision" of the Sequence
 * <p/>
 * DF: Added a "P" in front of "value" because Enum does not accept "number only" as value
 * 
 * @author Denis Forveille
 */
public enum DB2SequencePrecision implements DBPNamedObject {
    P5("Smallint", 5, "SMALLINT"),

    P10("Integer", 10, "INTEGER"),

    P19("Bigint", 19, "BIGINT");

    private String name;
    private Integer dataType;
    private String sqlKeyword;

    // -----------
    // Constructor
    // -----------
    private DB2SequencePrecision(String name, Integer dataType, String sqlKeyword)
    {
        this.name = name;
        this.dataType = dataType;
        this.sqlKeyword = sqlKeyword;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------

    @Override
    public String toString()
    {
        return name;
    }

    // ------------------------
    // Helpers
    // ------------------------
    public static DB2SequencePrecision getFromDataType(Integer dataType)
    {
        for (DB2SequencePrecision item : DB2SequencePrecision.values()) {
            if (dataType.equals(item.getDataType())) {
                return item;
            }
        }
        return null;
    }

    // ----------------
    // Standard Getters
    // ----------------

    public Integer getDataType()
    {
        return dataType;
    }

    public String getSqlKeyword()
    {
        return sqlKeyword;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

}