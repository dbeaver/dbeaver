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
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * DB2 Type of Constraints
 * 
 * @author Denis Forveille
 */
public enum DB2ConstraintType implements DBPNamedObject {
    F("Foreign key", DBSEntityConstraintType.FOREIGN_KEY),

    I("Functional dependency", DBSEntityConstraintType.ASSOCIATION),

    K("Check", DBSEntityConstraintType.CHECK),

    P("Primary key", DBSEntityConstraintType.PRIMARY_KEY),

    U("Unique", DBSEntityConstraintType.UNIQUE_KEY);

    private String name;
    private DBSEntityConstraintType type;

    // -----------
    // Constructor
    // -----------
    private DB2ConstraintType(String name, DBSEntityConstraintType type)
    {
        this.name = name;
        this.type = type;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // -----------
    // Helpers
    // -----------
    public static DBSEntityConstraintType getConstraintType(String code)
    {
        return DB2ConstraintType.valueOf(code).getType();
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

    public DBSEntityConstraintType getType()
    {
        return type;
    }

}