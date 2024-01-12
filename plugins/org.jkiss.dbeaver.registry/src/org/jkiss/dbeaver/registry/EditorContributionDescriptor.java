/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPEditorContribution;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EditorContributionDescriptor
 */
public class EditorContributionDescriptor extends AbstractContextDescriptor implements DBPEditorContribution
{
    private static final Log log = Log.getLog(EditorContributionDescriptor.class);

    private final String editorId;
    private final String category;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private JexlExpression visibleIf;

    private final List<String> supportedDataSources = new ArrayList<>();
    private final List<String> supportedDrivers = new ArrayList<>();

    public EditorContributionDescriptor(IConfigurationElement config)
    {
        super(config);

        this.editorId = config.getAttribute("editorId");
        this.category = config.getAttribute("category");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.icon = iconToImage(config.getAttribute("icon"));
        String visibleIfCondition = config.getAttribute(RegistryConstants.ATTR_VISIBLE_IF);

        if (!CommonUtils.isEmpty(visibleIfCondition)) {
            try {
                this.visibleIf = AbstractDescriptor.parseExpression(visibleIfCondition);
            } catch (DBException e) {
                log.debug("Error parsing expression '" + visibleIfCondition + "':" + GeneralUtils.getExpressionParseMessage(e));
            }
        }

        for (IConfigurationElement supportsCfg : config.getChildren("supports")) {
            String supportsDS = supportsCfg.getAttribute(RegistryConstants.ATTR_DATA_SOURCE);
            if (!CommonUtils.isEmpty(supportsDS)) {
                supportedDataSources.addAll(Arrays.asList(supportsDS.split(",")));
            }
            String supportsDrivers = supportsCfg.getAttribute(RegistryConstants.ATTR_DRIVER);
            if (!CommonUtils.isEmpty(supportsDrivers)) {
                supportedDrivers.addAll(Arrays.asList(supportsDrivers.split(",")));
            }
        }
    }

    @Override
    public String getEditorId() {
        return editorId;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

    public boolean isVisible(DBNNode context) {
        try {
            return visibleIf == null || Boolean.TRUE.equals(visibleIf.evaluate(DBNUtils.makeContext(context)));
        } catch (JexlException e) {
            log.debug("Error evaluating expression '" + visibleIf.getSourceText() + "' on node '" + context.getName() + "': " + GeneralUtils.getExpressionParseMessage(e));
            return false;
        }
    }

    public boolean supportsDataSource(DBPDataSourceContainer dataSource) {
        if (supportedDataSources.isEmpty() && supportedDrivers.isEmpty()) {
            return true;
        }
        if (!supportedDataSources.isEmpty()) {
            boolean supportsProvider = false;
            for (DBPDataSourceProviderDescriptor dspd = dataSource.getDriver().getProviderDescriptor(); dspd != null; dspd = dspd.getParentProvider()) {
                if (supportedDataSources.contains(dspd.getId())) {
                    supportsProvider = true;
                    break;
                }
            }
            return supportsProvider;
        }
        return supportedDrivers.contains(dataSource.getDriver().getId());
    }

    @Override
    public String toString() {
        return editorId + "(" + category + ")";
    }

}
