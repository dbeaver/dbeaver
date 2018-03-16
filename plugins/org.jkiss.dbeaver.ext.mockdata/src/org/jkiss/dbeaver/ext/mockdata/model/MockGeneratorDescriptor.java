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

package org.jkiss.dbeaver.ext.mockdata.model;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.datatype.DataTypeAbstractDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MockGeneratorDescriptor
 */
public class MockGeneratorDescriptor extends DataTypeAbstractDescriptor<MockValueGenerator> {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.mockGenerator"; //$NON-NLS-1$

    public static final String TAG_PRESET = "preset"; //NON-NLS-1

    private Preset preset;
    private String label;
    private String description;
    private final String link;
    private final String url;
    private final DBPImage icon;
    private List<DBPPropertyDescriptor> properties = new ArrayList<>();
    private List<Preset> presets = new ArrayList<>();

    public MockGeneratorDescriptor(IConfigurationElement config)
    {
        super(config, MockValueGenerator.class);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.link = config.getAttribute(RegistryConstants.ATTR_LINK);
        this.url = config.getAttribute(RegistryConstants.ATTR_URL);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));

        for (IConfigurationElement prop : config.getChildren(PropertyDescriptor.TAG_PROPERTY_GROUP)) {
            properties.addAll(PropertyDescriptor.extractProperties(prop));
        }

        for (IConfigurationElement preset : config.getChildren(TAG_PRESET)) {
            presets.add(new Preset(
                    preset.getAttribute("id"),
                    preset.getAttribute("label"),
                    preset.getAttribute("mnemonics"),
                    preset.getAttribute("description"),
                    PropertyDescriptor.extractProperties(preset)
            ));
        }
    }

    public MockGeneratorDescriptor(IConfigurationElement config, Preset preset) {
        this(config);

        this.preset = preset;
        this.label = "   " + preset.label;
        if (!CommonUtils.isEmpty(preset.description)) {
            this.description = preset.description;
        }
        for (DBPPropertyDescriptor prop : preset.getProperties()) {
            setDefaultProperty(prop.getId(), prop.getDefaultValue());
        }
        this.presets.clear();
    }

    private void setDefaultProperty(Object id, Object defaultValue) {
        for (DBPPropertyDescriptor property : properties) {
            if (property.getId().equals(id)) {
                ((PropertyDescriptor) property).setDefaultValue(defaultValue); break;
            }
        }

    }

    @Override
    public String getId() {
        if (preset != null) {
            return super.getId() + "_" + preset.id;
        } else {
            return super.getId();
        }
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

    public String getLink() {
        return link;
    }

    public String getUrl() {
        return url;
    }

    public List<DBPPropertyDescriptor> getProperties() {
        return properties;
    }

    public DBPPropertyDescriptor getProperty(Object id) {
        for (DBPPropertyDescriptor descriptor : getProperties()) {
            if (id.equals(descriptor.getId())) {
                return descriptor;
            }
        }
        return null;
    }

    @NotNull
    public MockValueGenerator createGenerator() {
        return createInstance();
    }

    public List<Preset> getPresets() {
        return presets;
    }

    public static class Preset {
        private final String id;
        private final String label;
        private final String mnemonics;
        private final String description;
        private final List<DBPPropertyDescriptor> properties;

        public Preset(String id, String label, String mnemonics, String description, List<DBPPropertyDescriptor> properties) {
            this.id = id;
            this.label = label;
            this.mnemonics = mnemonics;
            this.description = description;
            this.properties = properties;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String getMnemonics() {
            return mnemonics;
        }

        public String getDescription() {
            return description;
        }

        public List<DBPPropertyDescriptor> getProperties() {
            return properties;
        }
    }
}
