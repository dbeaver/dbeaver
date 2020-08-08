package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.serialize.DBPObjectSerializer;

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
    }

    @Override
    public DatabaseTransferConsumer deserializeObject(DBRRunnableContext runnableContext, DBTTask objectContext, Map<String, Object> state) throws DBCException {
        return new DatabaseTransferConsumer();
    }
}
