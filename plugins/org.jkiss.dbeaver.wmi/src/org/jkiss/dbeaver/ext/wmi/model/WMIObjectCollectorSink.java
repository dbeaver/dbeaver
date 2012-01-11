package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIObjectSink;
import org.jkiss.wmi.service.WMIObjectSinkStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WMIObjectCollectorSink implements WMIObjectSink
{
    private final DBRProgressMonitor monitor;
    private final List<WMIObject> objectList = new ArrayList<WMIObject>();
    private volatile boolean finished = false;

    public WMIObjectCollectorSink(DBRProgressMonitor monitor)
    {
        this.monitor = monitor;
    }

    public List<WMIObject> getObjectList()
    {
        return objectList;
    }

    public void indicate(WMIObject[] objects)
    {
        Collections.addAll(objectList, objects);
        monitor.subTask(String.valueOf(objectList.size()) + " objects loaded");
    }

    public void setStatus(WMIObjectSinkStatus status, int result, String param, WMIObject errorObject)
    {
        if (status == WMIObjectSinkStatus.complete) {
            finished = true;
        }
    }

    public void waitForFinish()
    {
        try {
            while (!monitor.isCanceled() && !finished) {
                Thread.sleep(100);
            }
            //service.cancelAsyncOperation(wmiObjectSink);
        } catch (InterruptedException e) {
            // do nothing
        }
    }
}
