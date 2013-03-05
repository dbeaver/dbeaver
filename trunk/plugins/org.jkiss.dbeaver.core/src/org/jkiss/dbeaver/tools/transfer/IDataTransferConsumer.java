package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;
import java.util.Map;

/**
 * Data consumer
 */
public interface IDataTransferConsumer<SETTINGS extends IDataTransferSettings, PROCESSOR extends IDataTransferProcessor>
    extends IDataTransferNode<SETTINGS>, DBDDataReceiver
{

    void initTransfer(DBSObject sourceObject, SETTINGS settings, PROCESSOR processor, Map<Object, Object> processorProperties);

    String getTargetName();

}
