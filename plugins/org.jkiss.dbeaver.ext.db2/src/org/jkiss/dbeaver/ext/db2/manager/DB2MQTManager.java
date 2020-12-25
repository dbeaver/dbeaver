/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2MaterializedQueryTable;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

/**
 * DB2 MQT Manager
 * 
 * @author Denis Forveille
 */
public class DB2MQTManager extends DB2AbstractDropOnlyManager<DB2MaterializedQueryTable, DB2Schema> {

    private static final String SQL_DROP = "DROP TABLE %s";

    @Override
    public String buildDropStatement(DB2MaterializedQueryTable db2View)
    {
        String fullyQualifiedName = db2View.getFullyQualifiedName(DBPEvaluationContext.DDL);
        return String.format(SQL_DROP, fullyQualifiedName);
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2MaterializedQueryTable> getObjectsCache(DB2MaterializedQueryTable db2MQT)
    {
        return db2MQT.getSchema().getMaterializedQueryTableCache();
    }
}
