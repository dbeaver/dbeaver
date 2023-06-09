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
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;
import org.jkiss.utils.CommonUtils;

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
        DBSObjectContainer container = object.getContainer();
        if (container == null) {
            container = object.getSettings().getContainer();
        }
        if (container == null) {
            return;
        }
        state.put("entityId", DBUtils.getObjectFullId(container));
    }

    @Override
    public DatabaseTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
        var consumer = new DatabaseTransferConsumer();

        var entityId = CommonUtils.toString(state.get("entityId"));
        var project = objectContext.getProject();
        try {
            consumer.setContainer((DBSObjectContainer) DBUtils.findObjectById(new VoidProgressMonitor(), project, entityId));
        } catch (DBException e) {
            log.error(e);
        }
        return consumer;
    }
}
