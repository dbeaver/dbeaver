package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Data consumer
 */
public interface IDataTransferConsumer extends DBDDataReceiver {

    void initTransfer(DBSObject sourceObject, IDataTransferSettings settings);

    String getTargetName();


}
