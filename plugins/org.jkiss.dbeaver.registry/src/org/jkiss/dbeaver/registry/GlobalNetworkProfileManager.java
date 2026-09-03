/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import com.google.gson.FormattingStyle;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileProvider;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global network profile manager.
 */
public final class GlobalNetworkProfileManager extends DBWNetworkProfileManager {
    public static final String CONFIG_FILE_NAME = "network-profiles.json";

    private static final Log log = Log.getLog(GlobalNetworkProfileManager.class);

    private final DBPPlatform platform;

    GlobalNetworkProfileManager(@NotNull DBPPlatform platform) {
        this.platform = platform;
        WorkspaceConfigEventManager.addConfigChangedListener(CONFIG_FILE_NAME, o -> reloadProfiles());
    }

    @NotNull
    @Override
    protected List<DBWNetworkProfile> loadProfiles() {
        try {
            String npConfig = DBWorkbench.getPlatform().getConfigurationController().loadConfigurationFile(CONFIG_FILE_NAME);
            if (!CommonUtils.isEmpty(npConfig)) {
                Map<String, Object> json = JSONUtils.GSON.fromJson(npConfig, JSONUtils.MAP_TYPE_TOKEN);
                return DataSourceParser.parseProfiles(
                    new DataSourceParser.ContextParameters(
                        null,
                        DBWorkbench.isMultiuserOrDistributed() ? new DataSourceConfigurationManagerBuffer() : null,
                        Map.of()
                    ),
                    json
                );
            }
        } catch (DBException e) {
            log.error("Error loading global network profiles", e);
        }
        return super.loadProfiles();
    }

    @Override
    public void saveSettings() {
        try {
            List<DBWNetworkProfile> profiles = getProfiles();
            DataSourceParser.ContextParameters contextParameters = new DataSourceParser.ContextParameters(
                null,
                DBWorkbench.isMultiuserOrDistributed() ? new DataSourceConfigurationManagerBuffer() : null,
                new LinkedHashMap<>()
            );
            StringWriter strWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(strWriter);
            jsonWriter.setFormattingStyle(FormattingStyle.PRETTY);
            jsonWriter.setIndent("\t");
            jsonWriter.beginObject();
            DataSourceParser.saveNetworkProfiles(contextParameters, jsonWriter, profiles);
            jsonWriter.endObject();
            jsonWriter.flush();
            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(
                CONFIG_FILE_NAME,
                strWriter.toString()
            );
            if (!DBWorkbench.isMultiuserOrDistributed()) {
                for (DBWNetworkProfile profile : profiles) {
                    profile.persistSecrets(DBSSecretController.getGlobalSecretController());
                }
            }
        } catch (IOException | DBException e) {
            log.error("Error saving global network profiles", e);
        }
    }

    public void detachProfile(
        @NotNull DBWNetworkProfile profile,
        @NotNull List<? extends DBPDataSourceContainer> dataSources
    ) throws DBException {
        if (!profile.isGlobal()) {
            throw new IllegalArgumentException("Global network profile expected");
        }
        for (DBPDataSourceContainer dataSource : dataSources) {
            var configuration = dataSource.getConnectionConfiguration();
            configuration.setConfigProfile(null);
            configuration.setHandlers(List.of());
            dataSource.getRegistry().updateDataSource(dataSource, false);
        }
    }

    @NotNull
    @Override
    protected DBSSecretController getSecretController() throws DBException {
        return DBSSecretController.getGlobalSecretController();
    }

    @Nullable
    @Override
    protected DBWNetworkProfileProvider getProfileProvider() {
        return RuntimeUtils.getObjectAdapter(platform, DBWNetworkProfileProvider.class);
    }
}
