/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

/**
 * DB2 Routine Manager
 * 
 * @author Denis Forveille
 */
public class DB2RoutineManager extends DB2AbstractDropOnlyManager<DB2Routine, DB2Schema> {

    private static final String SQL_DROP_FUNCTION = "DROP SPECIFIC FUNCTION %s";
    private static final String SQL_DROP_METHOD = "DROP SPECIFIC METHOD %s";
    private static final String SQL_DROP_PROCEDURE = "DROP SPECIFIC PROCEDURE %s";

    @Override
    public String buildDropStatement(DB2Routine db2Routine)
    {
        String fullyQualifiedName = db2Routine.getFullyQualifiedName(DBPEvaluationContext.DDL);
        switch (db2Routine.getType()) {
        case F:
            return String.format(SQL_DROP_FUNCTION, fullyQualifiedName);
        case M:
            return String.format(SQL_DROP_METHOD, fullyQualifiedName);
        case P:
            return String.format(SQL_DROP_PROCEDURE, fullyQualifiedName);
        default:
            throw new IllegalStateException(db2Routine.getType() + " not supported");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2Routine> getObjectsCache(DB2Routine db2Routine)
    {
        switch (db2Routine.getType()) {
        case F:
            return db2Routine.getSchema().getUdfCache();
        case M:
            return db2Routine.getSchema().getMethodCache();
        case P:
            return db2Routine.getSchema().getProcedureCache();
        default:
            throw new IllegalStateException(db2Routine.getType() + " is not a supported DB2RoutineType");
        }

    }

}
