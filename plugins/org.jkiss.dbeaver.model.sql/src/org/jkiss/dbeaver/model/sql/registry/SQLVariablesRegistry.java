/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.sql.internal.SQLModelActivator;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SQLVariablesRegistry {
    private static final Log log = Log.getLog(SQLVariablesRegistry.class);

    public static final String CONFIG_FILE_PREFIX = "sql-variables-"; //$NON-NLS-1$
    public static final String CONFIG_FILE_SUFFIX = ".json"; //$NON-NLS-1$
    public static final String CONFIG_FILE_TYPE_DRIVER = "driver"; //$NON-NLS-1$
    public static final String CONFIG_FILE_TYPE_CONNECTION = "con"; //$NON-NLS-1$

    private static final Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private static SQLVariablesRegistry registry;
    private final Map<DBPDriver, List<DBCScriptContext.VariableInfo>> driverVariables = new HashMap<>();
    private final Map<DBPDataSourceContainer, List<DBCScriptContext.VariableInfo>> connectionVariables = new HashMap<>();

    private SQLVariablesRegistry() {
    }

    public static synchronized SQLVariablesRegistry getInstance() {
        if (registry == null) {
            registry = new SQLVariablesRegistry();
            registry.loadVariables();
        }
        return registry;
    }

    private void loadVariables() {
        File configLocation = getConfigLocation();
        File[] configFiles = configLocation.listFiles((dir, name) -> name.startsWith(CONFIG_FILE_PREFIX));
        if (configFiles == null) {
            return;
        }
        for (File configFile : configFiles) {
            String configName = configFile.getName();
            if (!configName.endsWith(CONFIG_FILE_SUFFIX)) {
                log.debug("Skip variables config: bad file extension (" + configFile.getAbsolutePath() + ")");
                continue;
            }
            configName = configName.substring(CONFIG_FILE_PREFIX.length(), configName.length() - CONFIG_FILE_SUFFIX.length());
            String driverId = null, conId = null;
            if (configName.startsWith(CONFIG_FILE_TYPE_DRIVER)) {
                driverId = configName.substring(CONFIG_FILE_TYPE_DRIVER.length() + 1);
            } else if (configName.startsWith(CONFIG_FILE_TYPE_CONNECTION)) {
                conId = configName.substring(CONFIG_FILE_TYPE_DRIVER.length() + 1);
            } else {
                log.debug("Skip variables config: unrecognized variables target (" + configFile.getAbsolutePath() + ")");
                continue;
            }

            List<DBCScriptContext.VariableInfo> variables = loadVariablesFromFile(configFile);
            if (driverId != null) {
                DBPDriver driver = null;
                driverVariables.put(driver, variables);
            } else {
                DBPDataSourceContainer dataSource = null;
                connectionVariables.put(dataSource, variables);
            }
        }
    }

    private List<DBCScriptContext.VariableInfo> loadVariablesFromFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Map<String, Object> map = JSONUtils.parseMap(CONFIG_GSON, r);

                List<DBCScriptContext.VariableInfo> variables = new ArrayList<>();

                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    DBCScriptContext.VariableInfo variableInfo;
                    if (entry.getValue() instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                        Object value = valueMap.get("value");
                        String type = JSONUtils.getString(valueMap, "type");
                        if (value == null || type == null) {
                            log.debug("Invalid variable declaration (" + entry.getKey() + ")");
                            continue;
                        }
                        variableInfo = new DBCScriptContext.VariableInfo(
                            entry.getKey(),
                            value,
                            CommonUtils.valueOf(DBCScriptContext.VariableType.class, type, DBCScriptContext.VariableType.VARIABLE));
                    } else {
                        variableInfo = new DBCScriptContext.VariableInfo(
                            entry.getKey(),
                            entry.getValue(),
                            DBCScriptContext.VariableType.VARIABLE);
                    }

                    variables.add(variableInfo);
                }

                return variables;
            }
        } catch (IOException e) {
            log.error(e);
            return Collections.emptyList();
        }
    }

    private File getConfigLocation() {
        return SQLModelActivator.getInstance().getStateLocation().toFile();
    }

    @NotNull
    public List<DBCScriptContext.VariableInfo> getDriverVariables(DBPDriver driver) {
        List<DBCScriptContext.VariableInfo> variables = driverVariables.get(driver);
        return variables == null ? Collections.emptyList() : new ArrayList<>(variables);
    }

    @NotNull
    public List<DBCScriptContext.VariableInfo> getDataSourceVariables(DBPDataSourceContainer dataSource) {
        List<DBCScriptContext.VariableInfo> variables = connectionVariables.get(dataSource);
        if (variables == null) {
            return getDriverVariables(dataSource.getDriver());
        }
        List<DBCScriptContext.VariableInfo> result = new ArrayList<>(variables);
        result.addAll(getDriverVariables(dataSource.getDriver()));
        return result;
    }

    public void updateVariables(
        @Nullable DBPDriver driver,
        @Nullable DBPDataSourceContainer dataSource,
        @NotNull List<DBCScriptContext.VariableInfo> variables)
    {

    }

}
