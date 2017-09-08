/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.utils.CommonUtils;

/**
 * DatabaseProducerSettings
 */
public class DatabaseProducerSettings implements IDataTransferSettings {

    public enum ExtractType {
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
    public void loadSettings(IRunnableContext runnableContext, DataTransferSettings dataTransferSettings, IDialogSettings dialogSettings)
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
