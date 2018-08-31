/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-log.2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.format.sqlworkbenchj;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * External SQL formatter
 */
public class SQLWorkbenchJManager {

    private static final Log log = Log.getLog(SQLWorkbenchJManager.class);

    private static SQLWorkbenchJManager instance;
    private final File workbenchPath;
    private final URLClassLoader wbClassLoader;

    public static SQLWorkbenchJManager getInstance() {
        return instance;
    }

    static void initManager(File path) throws DBException {
        if (instance == null || !instance.workbenchPath.equals(path)) {
            initializeManager(path);
        }
    }

    public static void initializeManager(File path) throws DBException {
        instance = new SQLWorkbenchJManager(path);
    }

    public SQLWorkbenchJManager(File wbPath) throws DBException {
        this.workbenchPath = wbPath;
        try {
            File wbJar = new File(workbenchPath, "sqlworkbench.jar");

            wbClassLoader = new URLClassLoader(new URL[] { wbJar.toURI().toURL() });
            Class<?> wbManagerClass = wbClassLoader.loadClass("workbench.WbManager");

            wbManagerClass.getMethod("initConsoleMode").invoke(null);
        } catch (Exception e) {
            throw new DBException("Error initializing SQL Workbench/J manager", e);
        }
    }

    public String format(DBPDataSource dataSource, String source) throws DBException {
        try {
            Class<?> wbFormatterClass = wbClassLoader.loadClass("workbench.sql.formatter.WbSqlFormatter");

            String driverClassName = dataSource.getContainer().getDriver().getDriverClassName();
            String formatType = "mysql";
            if (driverClassName.contains("mysql")) {
                formatType = "mysql";
            } else if (driverClassName.contains("postgresql")) {
                formatType = "postgresql";
            } else if (driverClassName.contains("oracle")) {
                formatType = "oracle";
            }
            Object wbFormatterInstance = wbFormatterClass.getConstructor(CharSequence.class, String.class).newInstance(source, formatType);
            Object formatResult = wbFormatterClass.getMethod("getFormattedSql").invoke(wbFormatterInstance);
            if (formatResult != null) {
                return CommonUtils.toString(formatResult);
            }

            return source;
        } catch (Exception e) {
            throw new DBException("Error calling SQL Workbench/J formatter", e);
        }
    }

}