package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Data consumer
 */
public interface IDataTransferConsumer<SETTINGS extends IDataTransferSettings> extends IDataTransferNode<SETTINGS>, DBDDataReceiver {

    void initTransfer(DBSObject sourceObject, IDataTransferProcessor processor, SETTINGS settings);

    String getTargetName();

    Collection<IDataTransferProcessor> getAvailableProcessors(Collection<Class<?>> objectTypes);

}
