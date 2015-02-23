/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.utils.CommonUtils;

/**
 * DatabaseProducerSettings
 */
public class DatabaseProducerSettings implements IDataTransferSettings {

    enum ExtractType {
        SINGLE_QUERY,
        SEGMENTS
    }

    private static final int DEFAULT_SEGMENT_SIZE = 100000;

    private int segmentSize = DEFAULT_SEGMENT_SIZE;

    private boolean openNewConnections = true;
    private boolean queryRowCount = true;
    private ExtractType extractType = ExtractType.SINGLE_QUERY;

    public DatabaseProducerSettings()
    {
    }

    public int getSegmentSize()
    {
        return segmentSize;
    }

    public void setSegmentSize(int segmentSize)
    {
        if (segmentSize > 0) {
            this.segmentSize = segmentSize;
        }
    }

    public boolean isQueryRowCount()
    {
        return queryRowCount;
    }

    public void setQueryRowCount(boolean queryRowCount)
    {
        this.queryRowCount = queryRowCount;
    }

    public boolean isOpenNewConnections()
    {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections)
    {
        this.openNewConnections = openNewConnections;
    }

    public ExtractType getExtractType()
    {
        return extractType;
    }

    public void setExtractType(ExtractType extractType)
    {
        this.extractType = extractType;
    }

    @Override
    public void loadSettings(IRunnableContext runnableContext, IDialogSettings dialogSettings)
    {
        if (dialogSettings.get("extractType") != null) {
            try {
                extractType = ExtractType.valueOf(dialogSettings.get("extractType"));
            } catch (IllegalArgumentException e) {
                extractType = ExtractType.SINGLE_QUERY;
            }
        }
        try {
            segmentSize = dialogSettings.getInt("segmentSize");
        } catch (NumberFormatException e) {
            segmentSize = DEFAULT_SEGMENT_SIZE;
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("openNewConnections"))) {
            openNewConnections = dialogSettings.getBoolean("openNewConnections");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("queryRowCount"))) {
            queryRowCount = dialogSettings.getBoolean("queryRowCount");
        }
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings)
    {
        dialogSettings.put("extractType", extractType.name());
        dialogSettings.put("segmentSize", segmentSize);
        dialogSettings.put("openNewConnections", openNewConnections);
        dialogSettings.put("queryRowCount", queryRowCount);
    }
}
