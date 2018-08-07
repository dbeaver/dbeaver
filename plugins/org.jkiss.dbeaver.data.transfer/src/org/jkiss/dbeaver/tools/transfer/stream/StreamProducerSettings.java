/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;

/**
 * Stream transfer settings
 */
public class StreamProducerSettings implements IDataTransferSettings {

    public static final String PROP_EXTRACT_IMAGES = "extractImages";
    public static final String PROP_FILE_EXTENSION = "extension";
    public static final String PROP_FORMAT = "format";

    private String[] inputFiles = new String[0];


    @Override
    public void loadSettings(IRunnableContext runnableContext, DataTransferSettings dataTransferSettings, IDialogSettings dialogSettings) {
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings) {
    }

}
