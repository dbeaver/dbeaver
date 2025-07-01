/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

/**
 * Completion settings.
 * These settings are stored for each connection separately.
 */
public class AICompletionSettings {

    private static final Log log = Log.getLog(AICompletionSettings.class);

    private final DBPDataSourceContainer dataSourceContainer;
    protected final DBPPreferenceStore preferenceStore;
    private boolean metaTransferConfirmed;
    private boolean allowMetaTransfer;
    private AIDatabaseScope scope;
    private String[] customObjectIds;

    public AICompletionSettings(@NotNull DBPDataSourceContainer dataSourceContainer) {
        this(getPreferenceStore(), dataSourceContainer);
    }

    public AICompletionSettings(@NotNull DBPPreferenceStore preferenceStore, @NotNull DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
        this.preferenceStore = preferenceStore;
        loadSettings();
    }

    @NotNull
    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public boolean isMetaTransferConfirmed() {
        return metaTransferConfirmed;
    }

    public void setMetaTransferConfirmed(boolean metaTransferConfirmed) {
        this.metaTransferConfirmed = metaTransferConfirmed;
    }

    public boolean isAllowMetaTransfer() {
        return allowMetaTransfer;
    }

    public void setAllowMetaTransfer(boolean allowMetaTransfer) {
        this.allowMetaTransfer = allowMetaTransfer;
    }

    public AIDatabaseScope getScope() {
        return scope;
    }

    public void setScope(AIDatabaseScope scope) {
        this.scope = scope;
    }

    public String[] getCustomObjectIds() {
        return customObjectIds;
    }

    public void setCustomObjectIds(String[] customObjectIds) {
        this.customObjectIds = customObjectIds;
    }

    @NotNull
    private static BundlePreferenceStore getPreferenceStore() {
        return new BundlePreferenceStore("org.jkiss.dbeaver.model.ai");
    }

    private void loadSettings() {
        metaTransferConfirmed = preferenceStore.getBoolean(getParameterName(AIConstants.AI_META_TRANSFER_CONFIRMED));
        scope = CommonUtils.valueOf(
            AIDatabaseScope.class,
            preferenceStore.getString(getParameterName(AIConstants.AI_META_SCOPE)),
            AIDatabaseScope.CURRENT_SCHEMA);
        String csString = preferenceStore.getString(getParameterName(AIConstants.AI_META_CUSTOM));
        customObjectIds = CommonUtils.isEmpty(csString) ? new String[0] : csString.split(",");
    }

    public void saveSettings() {
        preferenceStore.setValue(getParameterName(AIConstants.AI_META_TRANSFER_CONFIRMED), metaTransferConfirmed);
        preferenceStore.setValue(getParameterName(AIConstants.AI_META_SCOPE), scope.name());
        if (ArrayUtils.isEmpty(customObjectIds)) {
            preferenceStore.setToDefault(getParameterName(AIConstants.AI_META_CUSTOM));
        } else {
            preferenceStore.setValue(getParameterName(AIConstants.AI_META_CUSTOM), String.join(",", customObjectIds));
        }
        try {
            preferenceStore.save();
        } catch (IOException e) {
            log.error(e);
        }
    }

    @NotNull
    protected String getParameterName(@NotNull String postfix) {
        return "ai-" + dataSourceContainer.getId() + "." + postfix;
    }

}
