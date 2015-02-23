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

    @Override
    public String getName()
    {
        return name;
    }

}