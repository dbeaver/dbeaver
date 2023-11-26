/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseSynonym;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class AltibaseSynonymManager extends SQLObjectEditor<GenericSynonym, GenericStructContainer> {

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    public boolean canDeleteObject(GenericSynonym object) {
        return true;
    }
    
    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        AltibaseSynonym object = (AltibaseSynonym) command.getObject();
        
        actions.add(
            new SQLDatabasePersistAction(
                "Drop synonym",
                "DROP " + object.getSynonymBody())
        );
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GenericSynonym> getObjectsCache(GenericSynonym object) {
        DBSObject parentObject = object.getParentObject();
        if (parentObject instanceof GenericStructContainer) {
            return ((GenericStructContainer) parentObject).getSynonymCache();
        }
        return null;
    }

    @Override
    protected GenericSynonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
            Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<GenericSynonym, GenericStructContainer>.ObjectCreateCommand command,
            Map<String, Object> options) throws DBException {
    }
}