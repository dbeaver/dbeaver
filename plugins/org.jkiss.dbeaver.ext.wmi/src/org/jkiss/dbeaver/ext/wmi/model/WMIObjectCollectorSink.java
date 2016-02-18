/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.wmi.service.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class WMIObjectCollectorSink implements WMIObjectSink
{
    static final Log log = Log.getLog(WMIObjectCollectorSink.class);

    private final DBRProgressMonitor monitor;
    private final WMIService service;
    private final List<WMIObject> objectList = new ArrayList<>();
    private final long firstRow;
    private final long maxRows;
    private volatile boolean finished = false;
    private long totalIndicated = 0;
    private String errorDesc;

    public WMIObjectCollectorSink(DBRProgressMonitor monitor, WMIService service)
    {
        this.monitor = monitor;
        this.service = service;
        this.firstRow = 0;
        this.maxRows = 0;
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

    @Override
    public void indicate(WMIObject[] objects)
    {
        if (finished) {
            return;
        }

        if (firstRow <= 0 && maxRows <= 0) {
            // Read everything
            Collections.addAll(objectList, objects);
            totalIndicated += objects.length;
        } else {
            // Add part (or all) of new objects
            int startPos, lastPos = objects.length - 1;
            if (firstRow > 0) {
                if (totalIndicated + objects.length < firstRow) {
                    totalIndicated += objects.length;
                    return;
                } else if (totalIndicated < firstRow) {
                    startPos = (int)(firstRow - totalIndicated);
                } else {
                    startPos = 0;
                }
            } else {
                startPos = 0;
            }
            totalIndicated += startPos;
            for (int i = startPos; i <= lastPos; i++) {
                if (objectList.size() >= maxRows) {
                    finished = true;
                    break;
                }
                objectList.add(objects[i]);
                totalIndicated++;
            }

            if (finished) {
                // We read everything so lets cancel the sink
                try {
                    service.cancelSink(this);
                } catch (WMIException e) {
                    log.warn(e);
                }
            }
            //Collections.addAll(objectList, objects);
            //totalIndicated += objects.length;
        }
        monitor.subTask(String.valueOf(objectList.size()) + " objects loaded");
    }

    @Override
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
            finished = true;
            service.cancelSink(this);
        }
        if (errorDesc != null) {
            throw new WMIException(errorDesc);
        }
    }

}
