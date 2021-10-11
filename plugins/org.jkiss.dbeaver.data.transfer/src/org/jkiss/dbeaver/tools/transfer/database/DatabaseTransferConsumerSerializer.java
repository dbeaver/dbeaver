/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * This serialized does nothing. All settings are stored in DatabaseConsumerSettings.
 * Data transfer job initializes all pipe nodes with their settings and passed input (producer) object.
 * Thus consumer can find its settings (by searching in ConsumerSettings by producer object).
 */
public class DatabaseTransferConsumerSerializer implements DBPObjectSerializer<DBTTask, DatabaseTransferConsumer> {

    private static final Log log = Log.getLog(DatabaseTransferConsumerSerializer.class);

    @Override
    public void serializeObject(DBRRunnableContext runnableContext, DBTTask context, DatabaseTransferConsumer object, Map<String, Object> state) {
        DBSObject databaseObject = object.getDatabaseObject();
        if (databaseObject != null) {
            state.put("targetProject", context.getProject().getName());
            state.put("targetEntityId", DBUtils.getObjectFullId(databaseObject));
        }
    }

    @Override
    public DatabaseTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
        final DBSObject[] targetObject = {null};
        try {
            runnableContext.run(true, true, monitor -> {
                String targetProjectName = CommonUtils.toString(state.get("targetProject"));
                String targetEntityId = CommonUtils.toString(state.get("targetEntityId"));
                try {
                    DBPProject project = objectContext.getProject();
                    if (CommonUtils.isNotEmpty(targetProjectName)) {
                        project = DBWorkbench.getPlatform().getWorkspace().getProject(targetProjectName);
                    }
                    if (CommonUtils.isNotEmpty(targetEntityId)) {
                        targetObject[0] = DBUtils.findObjectById(monitor, project, targetEntityId);
                        if (targetObject[0] == null) {
                            log.warn("Can't find database object '" + targetEntityId + "'");
                        }
                    }
                } catch (DBException e) {
                    log.warn("Can't find database object", e);
                }
            });
        } catch (InvocationTargetException e) {
            throw new DBCException("Error instantiating data consumer", e.getTargetException());
        } catch (InterruptedException e) {
            throw new DBCException("Task deserialization canceled", e);
        }
        DBSObject object = targetObject[0];
        if (object instanceof DBSDataManipulator) {
            return new DatabaseTransferConsumer((DBSDataManipulator) object);
        } else {
            return new DatabaseTransferConsumer();
        }
    }
}
