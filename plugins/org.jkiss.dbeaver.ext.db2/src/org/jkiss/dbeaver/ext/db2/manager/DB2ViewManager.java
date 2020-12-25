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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2View;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

/**
 * DB2 View Manager
 * 
 * @author Denis Forveille
 */
public class DB2ViewManager extends DB2AbstractDropOnlyManager<DB2View, DB2Schema> {

    private static final String SQL_DROP = "DROP VIEW %s";

    @Override
    public String buildDropStatement(DB2View db2View)
    {
        String fullyQualifiedName = db2View.getFullyQualifiedName(DBPEvaluationContext.DDL);
        return String.format(SQL_DROP, fullyQualifiedName);
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2View> getObjectsCache(DB2View db2View)
    {
        return db2View.getSchema().getViewCache();
    }
}
