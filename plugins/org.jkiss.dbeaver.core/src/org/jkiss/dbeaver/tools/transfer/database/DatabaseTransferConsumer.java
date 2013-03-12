package org.jkiss.dbeaver.tools.transfer.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;

import java.util.Map;

/**
* Stream transfer consumer
*/
public class DatabaseTransferConsumer implements IDataTransferConsumer<DatabaseConsumerSettings, IDataTransferProcessor> {

    static final Log log = LogFactory.getLog(DatabaseTransferConsumer.class);

    private DBSDataContainer sourceObject;
    private DatabaseConsumerSettings settings;

    public DatabaseTransferConsumer()
    {
    }

    @Override
    public void fetchStart(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
    {
        initExporter(context);

    }

    @Override
    public void fetchRow(DBCExecutionContext context, DBCResultSet resultSet) throws DBCException
    {
    }

    @Override
    public void fetchEnd(DBCExecutionContext context) throws DBCException
    {
        closeExporter();
    }

    @Override
    public void close()
    {
    }

    private void initExporter(DBCExecutionContext context) throws DBCException
    {
    }

    private void closeExporter()
    {
    }

    @Override
    public void initTransfer(DBSObject sourceObject, DatabaseConsumerSettings settings, IDataTransferProcessor processor, Map<Object, Object> processorProperties)
    {
        this.sourceObject = (DBSDataContainer)sourceObject;
        this.settings = settings;
    }

    @Override
    public void startTransfer(DBRProgressMonitor monitor)
    {
        // Create all necessary database objects
        DBSObjectContainer container = settings.getContainer();
        monitor.beginTask("Create necessary database objects", 1);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
        monitor.done();
/*
        if (container != null) {
            try {
                DBEObjectManager<?> objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(container.getChildType(VoidProgressMonitor.INSTANCE));
                if (objectManager instanceof DBEObjectMaker<?, ?>) {
                    //((DBEObjectMaker) objectManager).createNewObject()
                }
            } catch (DBException e) {
                log.error(e);
            }
        }
*/

    }

    @Override
    public void finishTransfer()
    {
    }

    @Override
    public String getTargetName()
    {
        DatabaseMappingContainer dataMapping = settings.getDataMapping(sourceObject);
        if (dataMapping == null) {
            return "?";
        }

        switch (dataMapping.getMappingType()) {
            case create: return dataMapping.getTargetName() + " [Create]";
            case existing: return dataMapping.getTargetName() + " [Insert]";
            case skip: return "[Skip]";
            default: return "?";
        }
    }

}
