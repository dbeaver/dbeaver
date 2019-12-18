/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

public class PostgreBackupSettings extends AbstractImportExportSettings<DBSObject> {

    private String compression;
    private String encoding;
    private boolean showViews;
    private boolean useInserts;
    private boolean noPrivileges;
    private boolean noOwner;

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isShowViews() {
        return showViews;
    }

    public void setShowViews(boolean showViews) {
        this.showViews = showViews;
    }

    public boolean isUseInserts() {
        return useInserts;
    }

    public void setUseInserts(boolean useInserts) {
        this.useInserts = useInserts;
    }

    public boolean isNoPrivileges() {
        return noPrivileges;
    }

    public void setNoPrivileges(boolean noPrivileges) {
        this.noPrivileges = noPrivileges;
    }

    public boolean isNoOwner() {
        return noOwner;
    }

    public void setNoOwner(boolean noOwner) {
        this.noOwner = noOwner;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.loadSettings(runnableContext, store);

        showViews = CommonUtils.getBoolean(store.getString("Postgre.export.showViews"), false);
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
        store.setValue("Postgre.export.showViews", showViews);
    }
}
