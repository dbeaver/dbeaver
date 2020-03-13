/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;

public abstract class AbstractImportExportSettings<BASE_OBJECT extends DBSObject> extends AbstractNativeToolSettings<BASE_OBJECT> {

    private File outputFolder = new File(RuntimeUtils.getUserHomeDir().getAbsolutePath());
    private String outputFilePattern;

    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public String getOutputFilePattern() {
        return outputFilePattern;
    }

    public void setOutputFilePattern(String outputFilePattern) {
        this.outputFilePattern = outputFilePattern;
    }

    public void fillExportObjectsFromInput() {

    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);
        this.outputFilePattern = store.getString("export.outputFilePattern");
        if (CommonUtils.isEmpty(this.outputFilePattern)) {
            this.outputFilePattern = "dump-${database}-${timestamp}.sql";
        }
        outputFolder = new File(CommonUtils.toString(store.getString("export.outputFolder"), outputFolder.getAbsolutePath()));
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);
        preferenceStore.setValue("export.outputFilePattern", this.outputFilePattern);
        preferenceStore.setValue("export.outputFolder", this.outputFolder.getAbsolutePath());
    }

}
