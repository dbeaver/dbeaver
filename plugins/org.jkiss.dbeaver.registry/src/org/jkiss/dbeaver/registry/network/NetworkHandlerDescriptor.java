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
package org.jkiss.dbeaver.registry.network;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * NetworkHandlerDescriptor
 */
public class NetworkHandlerDescriptor extends AbstractContextDescriptor implements DBWHandlerDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.networkHandler"; //$NON-NLS-1$
    private static final Log log = Log.getLog(NetworkHandlerDescriptor.class);

    private final String id;
    private final String label;
    private final String codeName;
    private final String description;
    private final DBWHandlerType type;
    private final boolean secured;
    private final ObjectType handlerType;
    private final int order;
    private final List<String> replacesIDs;
    private NetworkHandlerDescriptor replacedBy;
    private final DBPPropertyDescriptor[] properties;
    private final boolean isDistributed; // see getter
    private final boolean isDesktop;
    private final boolean isPinned;

    NetworkHandlerDescriptor(
        IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.codeName = config.getAttribute("codeName") == null ? this.id : config.getAttribute("codeName");
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.type = DBWHandlerType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE).toUpperCase(Locale.ENGLISH));
        this.secured = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_SECURED), false);
        this.handlerType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_HANDLER_CLASS));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER), 1);
        this.isDistributed = CommonUtils.getBoolean(config.getAttribute("distributed"), false);
        this.isDesktop = CommonUtils.getBoolean(config.getAttribute("desktop"), true);
        this.isPinned = CommonUtils.getBoolean(config.getAttribute("pinned"), false);

        this.replacesIDs = Arrays.stream(config.getChildren("replace"))
            .map(re -> re.getAttribute("id"))
            .collect(Collectors.toList());

        this.properties = Arrays.stream(config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))
            .map(PropertyDescriptor::extractProperties)
            .flatMap(List<DBPPropertyDescriptor>::stream)
            .toArray(DBPPropertyDescriptor[]::new);
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getCodeName() {
        return codeName;
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
    public DBWHandlerType getType() {
        return type;
    }

    @Override
    public boolean isSecured() {
        return secured;
    }

    @Override
    public DBPPropertyDescriptor[] getHandlerProperties() {
        return properties;
    }

    public int getOrder() {
        return order;
    }

    public boolean matches(DBPDriver driver) {
        try {
            return appliesTo(driver.getDataSourceProvider(), driver);
        } catch (Exception e) {
            log.debug(e);
            return false;
        }
    }

    @NotNull
    public ObjectType getHandlerType() {
        return handlerType;
    }

    @NotNull
    @Override
    public String getImplClassName() {
        return getHandlerType().getImplName();
    }

    @Override
    public <T extends DBWNetworkHandler> T createHandler(Class<T> impl)
        throws DBException {
        return handlerType.createInstance(impl);
    }

    public boolean replaces(NetworkHandlerDescriptor otherDesc) {
        return replacesIDs.contains(otherDesc.id);
    }

    @Override
    public String toString() {
        return id;
    }

    NetworkHandlerDescriptor getReplacedBy() {
        return replacedBy;
    }

    void setReplacedBy(NetworkHandlerDescriptor replacedBy) {
        this.replacedBy = replacedBy;
    }

    @Override
    public boolean isDistributed() {
        return isDistributed;
    }

    // Handler works in desktop application only
    public boolean isDesktopHandler() {
        return isDesktop;
    }

    public boolean isPinned() {
        return isPinned;
    }
}
