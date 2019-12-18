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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class MySQLExportSettings extends AbstractImportExportSettings<DBSObject> {

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    private DumpMethod method = DumpMethod.NORMAL;
    private boolean noCreateStatements;
    private boolean addDropStatements = true;
    private boolean disableKeys = true;
    private boolean extendedInserts = true;
    private boolean dumpEvents;
    private boolean comments;
    private boolean removeDefiner;
    private boolean binariesInHex;
    private boolean noData;
    private boolean showViews;

    public List<MySQLDatabaseExportInfo> objects = new ArrayList<>();

    public DumpMethod getMethod() {
        return method;
    }

    public void setMethod(DumpMethod method) {
        this.method = method;
    }

    public boolean isNoCreateStatements() {
        return noCreateStatements;
    }

    public void setNoCreateStatements(boolean noCreateStatements) {
        this.noCreateStatements = noCreateStatements;
    }

    public boolean isAddDropStatements() {
        return addDropStatements;
    }

    public void setAddDropStatements(boolean addDropStatements) {
        this.addDropStatements = addDropStatements;
    }

    public boolean isDisableKeys() {
        return disableKeys;
    }

    public void setDisableKeys(boolean disableKeys) {
        this.disableKeys = disableKeys;
    }

    public boolean isExtendedInserts() {
        return extendedInserts;
    }

    public void setExtendedInserts(boolean extendedInserts) {
        this.extendedInserts = extendedInserts;
    }

    public boolean isDumpEvents() {
        return dumpEvents;
    }

    public void setDumpEvents(boolean dumpEvents) {
        this.dumpEvents = dumpEvents;
    }

    public boolean isComments() {
        return comments;
    }

    public void setComments(boolean comments) {
        this.comments = comments;
    }

    public boolean isRemoveDefiner() {
        return removeDefiner;
    }

    public void setRemoveDefiner(boolean removeDefiner) {
        this.removeDefiner = removeDefiner;
    }

    public boolean isBinariesInHex() {
        return binariesInHex;
    }

    public void setBinariesInHex(boolean binariesInHex) {
        this.binariesInHex = binariesInHex;
    }

    public boolean isNoData() {
        return noData;
    }

    public void setNoData(boolean noData) {
        this.noData = noData;
    }

    public boolean isShowViews() {
        return showViews;
    }

    public void setShowViews(boolean showViews) {
        this.showViews = showViews;
    }

    public void setObjects(List<MySQLDatabaseExportInfo> objects) {
        this.objects = objects;
    }

    public List<MySQLDatabaseExportInfo> getObjects() {
        return objects;
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.loadSettings(runnableContext, store);
        noCreateStatements = CommonUtils.getBoolean(store.getString("MySQL.export.noCreateStatements"), false);
        addDropStatements = CommonUtils.getBoolean(store.getString("MySQL.export.addDropStatements"), true);
        disableKeys = CommonUtils.getBoolean(store.getString("MySQL.export.disableKeys"), true);
        extendedInserts = CommonUtils.getBoolean(store.getString("MySQL.export.extendedInserts"), true);
        dumpEvents = CommonUtils.getBoolean(store.getString("MySQL.export.dumpEvents"), false);
        comments = CommonUtils.getBoolean(store.getString("MySQL.export.comments"), false);
        removeDefiner = CommonUtils.getBoolean(store.getString("MySQL.export.removeDefiner"), false);
        binariesInHex = CommonUtils.getBoolean(store.getString("MySQL.export.binariesInHex"), false);
        noData = CommonUtils.getBoolean(store.getString("MySQL.export.noData"), false);
        showViews = CommonUtils.getBoolean(store.getString("MySQL.export.showViews"), false);
        if (CommonUtils.isEmpty(getExtraCommandArgs())) {
            // Backward compatibility
            setExtraCommandArgs(store.getString("MySQL.export.extraArgs"));
        }
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
        store.setValue("MySQL.export.noCreateStatements", noCreateStatements);
        store.setValue("MySQL.export.addDropStatements", addDropStatements);
        store.setValue("MySQL.export.disableKeys", disableKeys);
        store.setValue("MySQL.export.extendedInserts", extendedInserts);
        store.setValue("MySQL.export.dumpEvents", dumpEvents);
        store.setValue("MySQL.export.comments", comments);
        store.setValue("MySQL.export.removeDefiner", removeDefiner);
        store.setValue("MySQL.export.binariesInHex", binariesInHex);
        store.setValue("MySQL.export.noData", noData);
        store.setValue("MySQL.export.showViews", showViews);
    }

}
