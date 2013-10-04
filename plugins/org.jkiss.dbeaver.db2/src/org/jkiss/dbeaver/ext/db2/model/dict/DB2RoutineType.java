/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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

import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * DB2 Routine Type
 * 
 * @author Denis Forveille
 */
public enum DB2RoutineType implements DBPNamedObject {
    F("Function", DBSProcedureType.FUNCTION, DB2SourceType.FUNCTION),

    M("Method", DBSProcedureType.PROCEDURE, DB2SourceType.PROCEDURE),

    P("Procedure", DBSProcedureType.PROCEDURE, DB2SourceType.PROCEDURE);

    private String name;
    private DBSProcedureType procedureType;
    private DB2SourceType sourceType;

    // -----------
    // Constructor
    // -----------

    private DB2RoutineType(String name, DBSProcedureType procedureType, DB2SourceType sourceType)
    {
        this.name = name;
        this.procedureType = procedureType;
        this.sourceType = sourceType;
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
    // Standard Getters
    // ----------------

    @Override
    public String getName()
    {
        return name;
    }

    public DBSProcedureType getProcedureType()
    {
        return procedureType;
    }

    public DB2SourceType getSourceType()
    {
        return sourceType;
    }

}