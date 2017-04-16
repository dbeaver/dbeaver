/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultSetPresentationDescriptor
 */
public class ResultSetPanelDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.panel"; //NON-NLS-1 //$NON-NLS-1$

    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private final boolean showByDefault;
    private final List<IResultSetPresentation.PresentationType> supportedPresentationTypes = new ArrayList<>();
    private final List<String> supportedPresentations = new ArrayList<>();
    private final List<String> supportedDataSources = new ArrayList<>();

    protected ResultSetPanelDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.implClass = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_DEFAULT));

        for (IConfigurationElement supports : config.getChildren(TAG_SUPPORTS)) {
            String type = supports.getAttribute(RegistryConstants.ATTR_TYPE);
            if (!CommonUtils.isEmpty(type)) {
                supportedPresentationTypes.add(IResultSetPresentation.PresentationType.valueOf(type.toUpperCase(Locale.ENGLISH)));
            }
            String id = supports.getAttribute(RegistryConstants.ATTR_ID);
            if (!CommonUtils.isEmpty(id)) {
                supportedPresentations.add(id);
            }
        }

        for (IConfigurationElement dsElement : config.getChildren(RegistryConstants.TAG_DATASOURCE)) {
            String dsId = dsElement.getAttribute(RegistryConstants.ATTR_ID);
            if (dsId != null) {
                supportedDataSources.add(dsId);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isShowByDefault() {
        return showByDefault;
    }

    public boolean supportedBy(DBPDataSource dataSource, String presentationId, IResultSetPresentation.PresentationType presentationType) {
        if (!supportedDataSources.isEmpty()) {
            if (dataSource == null) {
                return false;
            }
            DriverDescriptor driver = (DriverDescriptor) dataSource.getContainer().getDriver();
            if (!supportedDataSources.contains(driver.getProviderDescriptor().getId())) {
                return false;
            }
        }
        if (supportedPresentations.isEmpty() && supportedPresentationTypes.isEmpty()) {
            return true;
        }
        return
            (presentationId != null && supportedPresentations.contains(presentationId)) ||
            (presentationType != null && supportedPresentationTypes.contains(presentationType));
    }

    public IResultSetPanel createInstance() throws DBException {
        return implClass.createInstance(IResultSetPanel.class);
    }

    @Override
    public String toString() {
        return id;
    }
}
