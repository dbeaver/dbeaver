/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object manager
 */
public abstract class AbstractObjectManager<OBJECT_TYPE extends DBSObject> implements DBEObjectManager<OBJECT_TYPE> {

    protected static final Log log = Log.getLog(AbstractObjectManager.class);

    @Override
    public void executePersistAction(DBCSession session, DBECommand<OBJECT_TYPE> command, DBEPersistAction action) throws DBException
    {
        String script = action.getScript();
        if (script == null) {
            action.handleExecute(session, null);
        } else {
            DBCStatement dbStat = DBUtils.createStatement(session, script, false);
            try {
                dbStat.executeStatement();
                action.handleExecute(session, null);
            } catch (DBCException e) {
                action.handleExecute(session, e);
                throw e;
            } finally {
                dbStat.close();
            }
        }
    }

    public static abstract class AbstractObjectReflector<OBJECT_TYPE extends DBSObject> implements DBECommandReflector<OBJECT_TYPE, DBECommand<OBJECT_TYPE>> {
        private final DBEObjectMaker<OBJECT_TYPE, ? extends DBSObject> objectMaker;
        protected AbstractObjectReflector(DBEObjectMaker<OBJECT_TYPE, ? extends DBSObject> objectMaker)
        {
            this.objectMaker = objectMaker;
        }
        protected void cacheModelObject(OBJECT_TYPE object)
        {
            DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = objectMaker.getObjectsCache(object);
            if (cache != null) {
                cache.cacheObject(object);
            }
        }
        protected void removeModelObject(OBJECT_TYPE object)
        {
            DBSObjectCache<? extends DBSObject, OBJECT_TYPE> cache = objectMaker.getObjectsCache(object);
            if (cache != null) {
                cache.removeObject(object, false);
            }
        }
    }

    public static class CreateObjectReflector<OBJECT_TYPE extends DBSObject> extends AbstractObjectReflector<OBJECT_TYPE> {
        public CreateObjectReflector(DBEObjectMaker<OBJECT_TYPE, ? extends DBSObject> objectMaker)
        {
            super(objectMaker);
        }

        @Override
        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            cacheModelObject(command.getObject());
            DBUtils.fireObjectAdd(command.getObject());
        }

        @Override
        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectRemove(command.getObject());
            removeModelObject(command.getObject());
        }
    }

    public static class DeleteObjectReflector<OBJECT_TYPE extends DBSObject> extends AbstractObjectReflector<OBJECT_TYPE> {
        public DeleteObjectReflector(DBEObjectMaker<OBJECT_TYPE, ? extends DBSObject> objectMaker)
        {
            super(objectMaker);
        }

        @Override
        public void redoCommand(DBECommand<OBJECT_TYPE> command)
        {
            DBUtils.fireObjectRemove(command.getObject());
            removeModelObject(command.getObject());
        }

        @Override
        public void undoCommand(DBECommand<OBJECT_TYPE> command)
        {
            cacheModelObject(command.getObject());
            DBUtils.fireObjectAdd(command.getObject());
        }

    }

}
