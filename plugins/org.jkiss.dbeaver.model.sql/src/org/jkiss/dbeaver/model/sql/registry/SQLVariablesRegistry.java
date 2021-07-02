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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.internal.SQLModelActivator;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SQLVariablesRegistry {
    private static final Log log = Log.getLog(SQLVariablesRegistry.class);

    public static final String CONFIG_FILE_PREFIX = "sql-variables-"; //$NON-NLS-1$
    public static final String CONFIG_FILE_SUFFIX = ".json"; //$NON-NLS-1$
    public static final String CONFIG_FILE_TYPE_DRIVER = "driver"; //$NON-NLS-1$
    public static final String CONFIG_FILE_TYPE_CONNECTION = "con"; //$NON-NLS-1$
    public static final String VARIABLES_STORE_DIR = "variables";

    private static final Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private static SQLVariablesRegistry registry;
    private final Map<DBPDriver, List<DBCScriptContext.VariableInfo>> driverVariables = new HashMap<>();
    private final Map<String, List<DBCScriptContext.VariableInfo>> connectionVariables = new HashMap<>();

    private ConfigSaver configSaver;
    private final List<Object> saveLock = new ArrayList<>();

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
//            configName = configName.substring(CONFIG_FILE_PREFIX.length(), configName.length() - CONFIG_FILE_SUFFIX.length());
//            String driverId = null, conId = null;
//            if (configName.startsWith(CONFIG_FILE_TYPE_DRIVER)) {
//                driverId = configName.substring(CONFIG_FILE_TYPE_DRIVER.length() + 1);
//            } else if (configName.startsWith(CONFIG_FILE_TYPE_CONNECTION)) {
//                conId = configName.substring(CONFIG_FILE_TYPE_DRIVER.length() + 1);
//            } else {
//                log.debug("Skip variables config: unrecognized variables target (" + configFile.getAbsolutePath() + ")");
//                continue;
//            }

            loadVariablesFromFile(configFile);
        }
    }

    private void loadVariablesFromFile(File file) {
        try (InputStream is = new FileInputStream(file)) {
            try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Map<String, Object> map = JSONUtils.parseMap(CONFIG_GSON, r);
                String driverId = JSONUtils.getString(map, "driver");
                String dataSourceId = JSONUtils.getString(map, "datasource");
                Map<String, Object> varSrc = JSONUtils.getObject(map, "variables");

                List<DBCScriptContext.VariableInfo> variables = new ArrayList<>();

                for (Map.Entry<String, Object> entry : varSrc.entrySet()) {
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

//                Map<String, DBCScriptContext.VariableInfo> varMap = new LinkedHashMap<>();
//                for (DBCScriptContext.VariableInfo v : variables) {
//                    varMap.put(v.name, v);
//                }

                if (driverId != null) {
                    DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(driverId);
                    if (driver == null) {
                        log.debug("Driver '" + driverId + "' not found. Saved variables ignored (" + file.getAbsolutePath() + ")");
                    } else {
                        this.driverVariables.put(driver, variables);
                    }
                } else if (dataSourceId != null) {
                    this.connectionVariables.put(dataSourceId, variables);
                }
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    private File getConfigLocation() {
        return new File(SQLModelActivator.getInstance().getStateLocation().toFile(), VARIABLES_STORE_DIR);
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
        if (dataSource != null) {
            List<DBCScriptContext.VariableInfo> vars = connectionVariables.get(dataSource.getId());
            if (vars == null) {
                connectionVariables.put(dataSource.getId(), new ArrayList<>(variables));
            } else {
                vars.addAll(variables);
            }
        } else if (driver != null) {
            List<DBCScriptContext.VariableInfo> vars = driverVariables.get(driver.getId());
            if (vars == null) {
                driverVariables.put(driver, new ArrayList<>(variables));
            } else {
                vars.addAll(variables);
            }
        }

        saveConfig(driver, dataSource);
    }

    void saveConfig(@Nullable DBPDriver driver,
                    @Nullable DBPDataSourceContainer dataSource)
    {
        synchronized (saveLock) {
            if (configSaver != null) {
                configSaver.cancel();
                configSaver = null;
            }
            if (driver != null && !saveLock.contains(driver)) saveLock.add(driver);
            if (dataSource != null && !saveLock.contains(dataSource)) saveLock.add(dataSource);
            configSaver = new ConfigSaver();
            configSaver.schedule(1000);
        }
    }


    private class ConfigSaver extends AbstractJob {
        ConfigSaver() {
            super("Tab folders configuration save");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            List<Object> toSave;
            synchronized (saveLock) {
                toSave = new ArrayList<>(saveLock);
                saveLock.clear();
            }
            flushConfig(toSave);
            return Status.OK_STATUS;
        }

        private void flushConfig(List<Object> toSave) {
            File configLocation = getConfigLocation();
            if (!configLocation.exists()) {
                if (!configLocation.mkdirs()) {
                    log.error("Error creating variables storage location: " + configLocation.getAbsolutePath());
                    return;
                }
            }

            for (Object so : toSave) {
                DBPDriver driver = null;
                DBPDataSourceContainer con = null;
                String fileName;
                if (so instanceof DBPDriver) {
                    driver = (DBPDriver) so;
                    fileName = CONFIG_FILE_PREFIX + CONFIG_FILE_TYPE_DRIVER + "-" + driver.getFullId() + CONFIG_FILE_SUFFIX;
                } else if (so instanceof DBPDataSourceContainer) {
                    con = (DBPDataSourceContainer)so;
                    fileName = CONFIG_FILE_PREFIX + CONFIG_FILE_TYPE_CONNECTION + "-" + ((DBPDataSourceContainer) so).getId() + CONFIG_FILE_SUFFIX;
                } else {
                    continue;
                }

                fileName = CommonUtils.escapeFileName(fileName);

                File configFile = new File(configLocation, fileName);
                saveConfigToFile(configFile, driver, con);
            }
        }

        private void saveConfigToFile(File configFile, DBPDriver driver, DBPDataSourceContainer con) {
            Map<String, Object> map = new LinkedHashMap<>();
            List<DBCScriptContext.VariableInfo> variables;
            if (driver != null) {
                map.put("driver", driver.getFullId());
                variables = driverVariables.get(driver);
            } else if (con != null) {
                map.put("datasource", con.getId());
                variables = connectionVariables.get(con.getId());
            } else {
                log.debug("Both driver and connection are null");
                return;
            }
            if (CommonUtils.isEmpty(variables)) {
                return;
            }
            Map<String, Object> varMap = new LinkedHashMap<>();
            for (DBCScriptContext.VariableInfo v : variables) {
                if (v.type == DBCScriptContext.VariableType.VARIABLE) {
                    varMap.put(v.name, v.value);
                } else {
                    Map<String, Object> varDetails = new LinkedHashMap<>();
                    varDetails.put("type", v.type.name());
                    varDetails.put("value", v.value);
                    varMap.put(v.name, varDetails);
                }
            }
            map.put("variables", varMap);

            try {
                IOUtils.writeFileFromString(configFile, CONFIG_GSON.toJson(map, Map.class));
            } catch (IOException e) {
                log.error(e);
            }
        }

    }

}
