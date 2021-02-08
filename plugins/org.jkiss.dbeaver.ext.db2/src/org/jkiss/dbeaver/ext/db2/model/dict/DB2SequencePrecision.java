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