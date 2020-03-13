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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.tasks.nativetool.AbstractScriptExecuteSettings;
import org.jkiss.utils.CommonUtils;

public class MySQLScriptExecuteSettings extends AbstractScriptExecuteSettings<MySQLCatalog> {

    public enum LogLevel {
        Normal,
        Verbose,
        Debug
    }

    private LogLevel logLevel = LogLevel.Normal;
    private boolean noBeep = true;

    private boolean isImport;

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isNoBeep() {
        return noBeep;
    }

    public void setNoBeep(boolean noBeep) {
        this.noBeep = noBeep;
    }

    public boolean isImport() {
        return isImport;
    }

    public void setImport(boolean anImport) {
        isImport = anImport;
    }

    public boolean isVerbose() {
        return logLevel == LogLevel.Verbose || logLevel == LogLevel.Debug;
    }

    @Override
    public MySQLServerHome findNativeClientHome(String clientHomeId) {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) throws DBException {
        super.loadSettings(runnableContext, preferenceStore);

        isImport = CommonUtils.toBoolean(preferenceStore.getString("MySQL.script.import"));
        logLevel = CommonUtils.valueOf(LogLevel.class, preferenceStore.getString("MySQL.script.logLevel"), LogLevel.Normal);
        noBeep = CommonUtils.toBoolean(preferenceStore.getString("MySQL.script.noBeep"));
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);

        preferenceStore.setValue("MySQL.script.import", isImport);
        preferenceStore.setValue("MySQL.script.logLevel", logLevel.name());
        preferenceStore.setValue("MySQL.script.noBeep", noBeep);
    }
}
