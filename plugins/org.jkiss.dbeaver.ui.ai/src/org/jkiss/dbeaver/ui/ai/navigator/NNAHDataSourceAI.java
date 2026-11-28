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
package org.jkiss.dbeaver.ui.ai.navigator;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIIcons;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.preferences.PrefPageDataSourceAI;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorNodeActionHandlerAbstract;

/**
 * Opens connection AI settings from the database navigator.
 */
public class NNAHDataSourceAI extends NavigatorNodeActionHandlerAbstract {
    @Override
    public boolean isEnabledFor(@NotNull DBNNode node) {
        return node instanceof DBNDataSource dataSource
            && new AIContextSettingsDataSource(dataSource.getDataSourceContainer()).isMetaTransferConfirmed();
    }

    @NotNull
    @Override
    public DBPImage getNodeActionIcon(@NotNull DBNNode node) {
        return AIIcons.AI_SETTINGS;
    }

    @Nullable
    @Override
    public String getNodeActionToolTip(@NotNull DBNNode node) {
        if (!(node instanceof DBNDataSource dataSource)) {
            return null;
        }
        AIContextSettingsDataSource settings = new AIContextSettingsDataSource(dataSource.getDataSourceContainer());
        String scope = settings.getScope().getTitle();
        if (settings.getScope() == AIDatabaseScope.CUSTOM) {
            String[] objects = settings.getCustomObjectIds();
            scope += " (" + NLS.bind(AIUIMessages.scope_selector_custom_objects, objects == null ? 0 : objects.length) + ")";
        }
        String tooltip = NLS.bind(
            AIUIMessages.ai_navigator_settings_tooltip, getEnablement(settings.isMetaTransferConfirmed()), scope);
        if (settings.isMcpEnabled()) {
            tooltip += "\n" + AIUIMessages.ai_navigator_settings_mcp;
        }
        if (settings.isDatadamMcpEnabled()) {
            tooltip += "\n" + AIUIMessages.ai_navigator_settings_datadam_mcp;
        }
        return tooltip;
    }

    @Override
    public void handleNodeAction(@NotNull DBNNode node, boolean defaultAction) {
        if (node instanceof DBNDataSource dataSource) {
            UIServiceConnections service = DBWorkbench.getService(UIServiceConnections.class);
            if (service != null) {
                service.openConnectionEditor(dataSource.getDataSourceContainer(), PrefPageDataSourceAI.class.getName());
            }
        }
    }

    @NotNull
    private static String getEnablement(boolean enabled) {
        return enabled ? AIUIMessages.ai_navigator_settings_enabled : AIUIMessages.ai_navigator_settings_disabled;
    }
}
