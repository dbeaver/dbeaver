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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.tasks.nativetool.AbstractScriptExecuteSettings;
import org.jkiss.utils.CommonUtils;

public class MySQLScriptExecuteSettings extends AbstractScriptExecuteSettings<MySQLCatalog> implements MySQLNativeCredentialsSettings {
    private static final String PREFERENCE_PREFIX = "MySQL.script.";
    private static final String PREFERENCE_IS_IMPORT = PREFERENCE_PREFIX + ".import";
    private static final String PREFERENCE_LOG_LEVEL = PREFERENCE_PREFIX + "logLevel";
    private static final String PREFERENCE_NO_BEEP = PREFERENCE_PREFIX + "noBeep";
    private static final String PREFERENCE_DISABLE_FOREIGN_KEY = PREFERENCE_PREFIX + "disableForeignKey";

    public enum LogLevel {
        Normal,
        Verbose,
        Debug
    }

    private LogLevel logLevel = LogLevel.Normal;
    private boolean noBeep = true;

    private boolean isImport;

    private boolean isForeignKeyCheckDisabled;

    private boolean overrideCredentials;

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

    public void setIsForeignKeyCheckDisabled(boolean isForeignKeyCheckDisabled) {
        this.isForeignKeyCheckDisabled = isForeignKeyCheckDisabled;
    }

    public boolean isForeignKeyCheckDisabled() {
        return isForeignKeyCheckDisabled;
    }

    @Override
    public boolean isOverrideCredentials() {
        return overrideCredentials;
    }

    @Override
    public void setOverrideCredentials(boolean value) {
        this.overrideCredentials = value;
    }

    @Override
    public MySQLServerHome findNativeClientHome(String clientHomeId) {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) throws DBException {
        super.loadSettings(runnableContext, preferenceStore);

        isImport = CommonUtils.getBoolean(preferenceStore.getString(PREFERENCE_IS_IMPORT), isImport);
        logLevel = CommonUtils.valueOf(LogLevel.class, preferenceStore.getString(PREFERENCE_LOG_LEVEL), LogLevel.Normal);
        noBeep = CommonUtils.toBoolean(preferenceStore.getString(PREFERENCE_NO_BEEP));
        isForeignKeyCheckDisabled = CommonUtils.toBoolean(preferenceStore.getString(PREFERENCE_DISABLE_FOREIGN_KEY));
        overrideCredentials = CommonUtils.toBoolean(preferenceStore.getString(MySQLNativeCredentialsSettings.PREFERENCE_NAME));
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);

        preferenceStore.setValue(PREFERENCE_IS_IMPORT, isImport);
        preferenceStore.setValue(PREFERENCE_LOG_LEVEL, logLevel.name());
        preferenceStore.setValue(PREFERENCE_NO_BEEP, noBeep);
        preferenceStore.setValue(PREFERENCE_DISABLE_FOREIGN_KEY, isForeignKeyCheckDisabled);
        preferenceStore.setValue(MySQLNativeCredentialsSettings.PREFERENCE_NAME, overrideCredentials);
    }
}
