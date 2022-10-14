/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.auth;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.auth.AuthPropertyDescriptor;
import org.jkiss.dbeaver.model.auth.SMAuthProvider;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.security.SMAuthCredentialsProfile;
import org.jkiss.dbeaver.model.security.SMAuthProviderDescriptor;
import org.jkiss.dbeaver.model.security.SMSubjectType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth service descriptor
 */
public class AuthProviderDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.auth.provider"; //$NON-NLS-1$
    private static final Log log = Log.getLog(AuthProviderDescriptor.class);

    private final IConfigurationElement cfg;

    private final ObjectType implType;
    private SMAuthProvider<?> instance;
    private final DBPImage icon;
    private final Map<String, PropertyDescriptor> configurationParameters = new LinkedHashMap<>();
    private final List<SMAuthCredentialsProfile> credentialProfiles = new ArrayList<>();
    private final boolean configurable;
    private final boolean trusted;
    private final String[] requiredFeatures;

    public AuthProviderDescriptor(IConfigurationElement cfg) {
        super(cfg);
        this.cfg = cfg;
        this.implType = new ObjectType(cfg, "class");
        this.icon = iconToImage(cfg.getAttribute("icon"));
        this.configurable = CommonUtils.toBoolean(cfg.getAttribute("configurable"));
        this.trusted = CommonUtils.toBoolean(cfg.getAttribute("trusted"));

        for (IConfigurationElement cfgElement : cfg.getChildren("configuration")) {
            for (IConfigurationElement propGroup : ArrayUtils.safeArray(cfgElement.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP))) {
                String category = propGroup.getAttribute(PropertyDescriptor.ATTR_LABEL);
                IConfigurationElement[] propElements = propGroup.getChildren(PropertyDescriptor.TAG_PROPERTY);
                for (IConfigurationElement prop : propElements) {
                    PropertyDescriptor propertyDescriptor = new PropertyDescriptor(category, prop);
                    configurationParameters.put(CommonUtils.toString(propertyDescriptor.getId()), propertyDescriptor);
                }
            }
        }
        for (IConfigurationElement credElement : cfg.getChildren("credentials")) {
            credentialProfiles.add(new SMAuthCredentialsProfile(credElement));
        }

        String rfList = cfg.getAttribute("requiredFeatures");
        if (!CommonUtils.isEmpty(rfList)) {
            requiredFeatures = rfList.split(",");
        } else {
            requiredFeatures = null;
        }
    }

    @NotNull
    public String getId() {
        return cfg.getAttribute("id");
    }

    public String getLabel() {
        return cfg.getAttribute("label");
    }

    public String getDescription() {
        return cfg.getAttribute("description");
    }

    public DBPImage getIcon() {
        return icon;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public List<PropertyDescriptor> getConfigurationParameters() {
        return new ArrayList<>(configurationParameters.values());
    }

    public List<SMAuthCredentialsProfile> getCredentialProfiles() {
        return new ArrayList<>(credentialProfiles);
    }

    public List<AuthPropertyDescriptor> getCredentialParameters(String[] propNames) {
        if (credentialProfiles.size() > 1) {
            for (SMAuthCredentialsProfile profile : credentialProfiles) {
                if (profile.getCredentialParameters().size() == propNames.length) {
                    boolean matches = true;
                    for (String paramName : propNames) {
                        if (profile.getCredentialParameter(paramName) == null) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        return profile.getCredentialParameters();
                    }
                }
            }
        }
        return credentialProfiles.get(0).getCredentialParameters();
    }

    @NotNull
    public SMAuthProvider<?> getInstance() {
        if (instance == null) {
            try {
                instance = implType.createInstance(SMAuthProvider.class);
            } catch (DBException e) {
                throw new IllegalStateException("Can not instantiate auth provider '" + implType.getImplName() + "'", e);
            }
        }
        return instance;
    }

    public String[] getRequiredFeatures() {
        return requiredFeatures;
    }

    @Override
    public String toString() {
        return getId();
    }

    public SMAuthProviderDescriptor createDescriptorBean() {
        SMAuthProviderDescriptor smInfo = new SMAuthProviderDescriptor();
        smInfo.setId(this.getId());
        smInfo.setLabel(this.getLabel());
        smInfo.setDescription(this.getDescription());
        smInfo.setCredentialProfiles(this.getCredentialProfiles());

        if (this.icon != null) {
            smInfo.setIcon(icon.getLocation());
        }

        return smInfo;
    }

    public List<DBPPropertyDescriptor> getMetaProperties(SMSubjectType subjectType) {
        return null;
    }

}
