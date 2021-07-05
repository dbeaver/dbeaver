/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

public class PostgreBackupRestoreSettings extends AbstractImportExportSettings<DBSObject> {
    public enum ExportFormat {
        PLAIN("p", "Plain", "sql"),
        CUSTOM("c", "Custom", ""),
        DIRECTORY("d", "Directory", ""),
        TAR("t", "Tar", "tar");

        private final String id;
        private String title;
        @NotNull
        private final String ext;

        ExportFormat(String id, String title, @NotNull String ext) {
            this.id = id;
            this.title = title;
            this.ext = ext;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        @NotNull
        public String getExt() {
            return ext;
        }
    }

    private ExportFormat format = ExportFormat.CUSTOM;

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);

        this.format = CommonUtils.valueOf(ExportFormat.class, store.getString("pg.format"), ExportFormat.CUSTOM);
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);

        store.setValue("pg.format", format == null ? null : format.name());
    }

}
