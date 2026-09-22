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

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSourceTypeDescriptor extends AbstractDescriptor implements DBPDataSourceType {
    private final String id;
    private final String name;
    private final String description;
    private final String dataSourceInformation;
    private final DBPImage icon;
    private final DBPImage iconBig;
    private final DBPImage logoImage;
    private final List<DBPDriver> drivers = new ArrayList<>();

    DataSourceTypeDescriptor(
        @NotNull String id,
        @NotNull String name,
        @Nullable String description,
        @NotNull String dataSourceInformation,
        @NotNull DBPImage icon
    ) {
        super("org.jkiss.dbeaver.registry");
        this.id = id;
        this.name = name;
        this.description = description;
        this.dataSourceInformation = dataSourceInformation;
        this.icon = icon;
        this.iconBig = icon;
        this.logoImage = null;
    }

    DataSourceTypeDescriptor(@NotNull IConfigurationElement config) {
        super(config);
        id = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_ID));
        String label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        name = CommonUtils.isEmpty(label) ? id : label;
        description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        String information = config.getAttribute(RegistryConstants.ATTR_DATA_SOURCE_INFORMATION);
        dataSourceInformation = CommonUtils.isEmpty(information) ? name : information;
        DBPImage smallIcon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        icon = smallIcon == null ? DBIcon.DATABASE_DEFAULT : smallIcon;
        DBPImage largeIcon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON_BIG));
        iconBig = largeIcon == null ? icon : largeIcon;
        logoImage = iconToImage(config.getAttribute("logoImage"));
    }

    DataSourceTypeDescriptor(@NotNull String id, @NotNull DBPDriver driver) {
        super(driver.getProviderDescriptor().getPluginId());
        this.id = id;
        name = CommonUtils.isEmpty(driver.getName()) ? id : driver.getName();
        description = driver.getDescription();
        dataSourceInformation = name;
        icon = driver.getPlainIcon();
        iconBig = driver.getIconBig();
        logoImage = driver.getLogoImage();
    }

    void addDriver(@NotNull DBPDriver driver) {
        if (!drivers.contains(driver)) {
            drivers.add(driver);
        }
    }

    void removeDriver(@NotNull DBPDriver driver) {
        drivers.remove(driver);
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @NotNull String getDataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public @NotNull DBPImage getIcon() {
        return icon;
    }

    @Override
    public @NotNull DBPImage getIconBig() {
        return iconBig;
    }

    @Override
    public @Nullable DBPImage getLogoImage() {
        return logoImage;
    }

    @Override
    public @NotNull List<? extends DBPDriver> getDrivers() {
        return List.copyOf(drivers);
    }

    @Override
    public @NotNull List<? extends DBPDriver> getEnabledDrivers() {
        return drivers.stream()
            .filter(driver -> driver.getProviderDescriptor().getDrivers().contains(driver))
            .filter(driver -> !driver.isDisabled() && driver.getReplacedBy() == null && driver.isSupportedByLocalSystem())
            .toList();
    }

    @Override
    public int getPromotedScore() {
        return getEnabledDrivers().stream().mapToInt(DBPDriver::getPromotedScore).sum();
    }
}
