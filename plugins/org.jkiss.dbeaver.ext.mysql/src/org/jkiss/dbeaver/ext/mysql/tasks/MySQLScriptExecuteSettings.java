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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.tasks.nativetool.AbstractScriptExecuteSettings;

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

}
