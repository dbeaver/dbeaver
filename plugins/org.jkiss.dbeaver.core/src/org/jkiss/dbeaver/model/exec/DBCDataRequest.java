package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;

import java.util.List;

/**
 * Data request
 */
public interface DBCDataRequest {

    void setDataReceiver(DBDDataReceiver dataReceiver);

    void setDataFilter(DBDDataFilter dataFilter);

    void setLimit(long firstRow, long maxRows);

    void setKeys(List<DBDAttributeValue> attributes);

    void setData(List<DBDAttributeValue> attributes);

    long execute(DBCExecutionContext context);

    long getResult();

    String generateScript(DBCExecutionContext context);

}
