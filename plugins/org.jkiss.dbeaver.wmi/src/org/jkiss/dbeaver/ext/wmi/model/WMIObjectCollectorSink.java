/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WMIObjectCollectorSink implements WMIObjectSink
{
    static final Log log = LogFactory.getLog(WMIObjectCollectorSink.class);

    private final DBRProgressMonitor monitor;
    private final WMIService service;
    private final List<WMIObject> objectList = new ArrayList<WMIObject>();
    private volatile boolean finished = false;
    private long totalIndicated = 0;
    private long firstRow;
    private long maxRows;
    private String errorDesc;

    public WMIObjectCollectorSink(DBRProgressMonitor monitor, WMIService service)
    {
        this.monitor = monitor;
        this.service = service;
    }

    WMIObjectCollectorSink(DBRProgressMonitor monitor, WMIService service, long firstRow, long maxRows)
    {
        this.monitor = monitor;
        this.service = service;
        this.firstRow = firstRow;
        this.maxRows = maxRows;
    }

    public List<WMIObject> getObjectList()
    {
        return objectList;
    }

    public void indicate(WMIObject[] objects)
    {
        if (firstRow <= 0 && maxRows <= 0) {
            Collections.addAll(objectList, objects);
            totalIndicated += objects.length;
        } else {
            Collections.addAll(objectList, objects);
            totalIndicated += objects.length;
        }
        monitor.subTask(String.valueOf(objectList.size()) + " objects loaded");
    }

    public void setStatus(WMIObjectSinkStatus status, int result, String param, WMIObject errorObject)
    {
        if (status == WMIObjectSinkStatus.complete || status == WMIObjectSinkStatus.error) {
            finished = true;
            if (status == WMIObjectSinkStatus.error) {
                errorDesc = param;
            }
        }
        if (errorObject != null) {
            errorObject.release();
        }
    }

    public void waitForFinish()
        throws WMIException
    {
        try {
            while (!monitor.isCanceled() && !finished) {
                Thread.sleep(100);
            }
            //service.cancelAsyncOperation(wmiObjectSink);
        } catch (InterruptedException e) {
            // do nothing
        }
        if (monitor.isCanceled()) {
            service.cancelSink(this);
        }
        if (errorDesc != null) {
            throw new WMIException(errorDesc);
        }
    }

}
