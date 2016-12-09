/*
 * DBeaver - Universal Database Manager
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
