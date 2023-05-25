/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Auth model descriptor
 */
public class DataSourceAuthModelDescriptor extends DataSourceBindingDescriptor implements DBPAuthModelDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataSourceAuth"; //$NON-NLS-1$

    private final String id;
    private final ObjectType implType;
    private final String name;
    private final String description;
    private final String requiredAuthProvider;
    private DBPImage icon;
    private final boolean defaultModel;
    private final boolean isDesktop;
    private final boolean isCloud;
    private final boolean requiresLocalConfiguration;
    private final Map<String, String[]> replaces = new HashMap<>();
    private boolean hasCondReplaces = false;

    private DBAAuthModel<?> instance;

    DataSourceAuthModelDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        if (this.icon == null) {
            this.icon = DBIcon.TREE_PACKAGE;
        }
        this.defaultModel = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_DEFAULT));
        this.isDesktop = CommonUtils.toBoolean(config.getAttribute("desktop"));
        this.isCloud = CommonUtils.toBoolean(config.getAttribute("cloud"));
        this.requiresLocalConfiguration = CommonUtils.toBoolean(config.getAttribute("requiresLocalConfiguration"));
        this.requiredAuthProvider = CommonUtils.toString(config.getAttribute("requiredAuthProvider"));
        for (IConfigurationElement dsConfig : config.getChildren("replace")) {
            String replModel = dsConfig.getAttribute("model");
            String forAttr = dsConfig.getAttribute("for");
            String[] replFor = CommonUtils.isEmpty(forAttr) ? new String[0] : forAttr.split(",");
            this.replaces.put(replModel, replFor);
            this.hasCondReplaces = hasCondReplaces || !ArrayUtils.isEmpty(replFor);
        }
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

    @NotNull
    @Override
    public String getImplClassName() {
        return implType.getImplName();
    }

    @Override
    public boolean isDefaultModel() {
        return defaultModel;
    }

    @Override
    public boolean isDesktopModel() {
        return isDesktop;
    }

    @Override
    public boolean isCloudModel() {
        return isCloud;
    }

    @Override
    public boolean requiresLocalConfiguration() {
        return requiresLocalConfiguration;
    }

    @Override
    public boolean isApplicableTo(DBPDriver driver) {
        return appliesTo(driver);
    }

    @Nullable
    @Override
    public DBPAuthModelDescriptor getReplacedBy(@NotNull DBPDriver driver) {
        // This is a bit tricky
        // We need to find all replacements (including inherited drivers)
        // And the remove all models which are not applicable to the driver

        List<? extends DBPAuthModelDescriptor> applicableAMs = DataSourceProviderRegistry.getInstance().getApplicableAuthModels(driver);

        List<DataSourceAuthModelDescriptor> allAuthModels = DataSourceProviderRegistry.getInstance().getAllAuthModels();
        for (int i = allAuthModels.size(); i > 0; i--) {
            DataSourceAuthModelDescriptor amd = allAuthModels.get(i - 1);
            if (applicableAMs.contains(amd) && amd.getReplaces(driver).contains(id) && amd.isDriverApplicable(driver)) {
                return amd;
            }
        }
        return null;
    }

    @NotNull
    public <T extends DBAAuthCredentials> DBAAuthModel<T> getInstance() {
        if (instance == null) {
            try {
                // locate class
                this.instance = implType.createInstance(DBAAuthModel.class);
            } catch (Throwable ex) {
                this.instance = null;
                throw new IllegalStateException("Can't initialize data source auth model '" + implType.getImplName() + "'", ex);
            }
        }
        return (DBAAuthModel<T>) instance;
    }

    @NotNull
    @Override
    public DBPPropertySource createCredentialsSource(DBPDataSourceContainer dataSource, DBPConnectionConfiguration configuration) {
        DBAAuthModel<?> instance = getInstance();
        DBAAuthCredentials credentials = dataSource == null || configuration == null ?
            instance.createCredentials() :
            instance.loadCredentials(dataSource, configuration);
        PropertyCollector propertyCollector = new PropertyCollector(credentials, false);
        propertyCollector.collectProperties();
        return propertyCollector;
    }

    @Nullable
    @Override
    public String getRequiredAuthProviderId() {
        return requiredAuthProvider;
    }

    boolean appliesTo(DBPDriver driver) {
        return isDriverApplicable(driver);
    }

    public Collection<String> getReplaces(DBPDriver driver) {
        if (hasCondReplaces) {
            List<String> replList = new ArrayList<>();
            for (Map.Entry<String, String[]> re : replaces.entrySet()) {
                String[] forList = re.getValue();
                if (!ArrayUtils.isEmpty(forList)) {
                    if (!ArrayUtils.contains(forList, driver.getId()) &&
                        !ArrayUtils.contains(forList, driver.getProviderId()))
                    {
                        continue;
                    }
                }
                replList.add(re.getKey());
            }
            return replList;
        } else {
            return replaces.keySet();
        }
    }

    @Override
    public String toString() {
        return id;
    }

}
