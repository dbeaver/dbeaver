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
package org.jkiss.dbeaver.registry.formatter;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFormatterRegistry implements DBPDataFormatterRegistry
{
    private static final Log log = Log.getLog(DataFormatterRegistry.class);

    public static final String CONFIG_FILE_NAME = "dataformat-profiles.xml"; //$NON-NLS-1$

    private static DataFormatterRegistry instance = null;

    public synchronized static DataFormatterRegistry getInstance()
    {
        if (instance == null) {
            instance = new DataFormatterRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<DataFormatterDescriptor> dataFormatterList = new ArrayList<>();
    private final Map<String, DataFormatterDescriptor> dataFormatterMap = new HashMap<>();
    private DBDDataFormatterProfile globalProfile;
    private List<DBDDataFormatterProfile> customProfiles = null;

    private DataFormatterRegistry(IExtensionRegistry registry)
    {
        // Load data formatters from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(DataFormatterDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                DataFormatterDescriptor formatterDescriptor = new DataFormatterDescriptor(ext);
                dataFormatterList.add(formatterDescriptor);
                dataFormatterMap.put(formatterDescriptor.getId(), formatterDescriptor);
            }
        }
    }

    public void dispose()
    {
        this.dataFormatterList.clear();
        this.dataFormatterMap.clear();
        this.globalProfile = null;
    }

    ////////////////////////////////////////////////////
    // Data formatters

    public List<DataFormatterDescriptor> getDataFormatters()
    {
        return dataFormatterList;
    }

    public DataFormatterDescriptor getDataFormatter(String typeId)
    {
        return dataFormatterMap.get(typeId);
    }

    @NotNull
    @Override
    public synchronized DBDDataFormatterProfile getGlobalProfile()
    {
        if (globalProfile == null) {
            globalProfile = new DataFormatterProfile(
                "Global",
                DBWorkbench.getPlatform().getPreferenceStore());
        }
        return globalProfile;
    }

    @Override
    @Nullable
    public synchronized DBDDataFormatterProfile getCustomProfile(@NotNull String name)
    {
        for (DBDDataFormatterProfile profile : getCustomProfilesInternal()) {
            if (profile.getProfileName().equals(name)) {
                return profile;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public synchronized List<DBDDataFormatterProfile> getCustomProfiles() {
        return List.copyOf(getCustomProfilesInternal());
    }

    private synchronized List<DBDDataFormatterProfile> getCustomProfilesInternal() {
        if (customProfiles == null) {
            loadProfiles();
            WorkspaceConfigEventManager.addConfigChangedListener(CONFIG_FILE_NAME, o -> {
                loadProfiles();
            });
        }
        return customProfiles;
    }

    private synchronized void loadProfiles() {
        customProfiles = new ArrayList<>();
        try {
            String content = DBWorkbench.getPlatform().getProductConfigurationController().loadConfigurationFile(CONFIG_FILE_NAME);
            if (CommonUtils.isEmpty(content)) {
                return;
            }
            try (StringReader is = new StringReader(content)) {
                SAXReader parser = new SAXReader(is);
                try {
                    parser.parse(new FormattersParser());
                } catch (Throwable ex) {
                    throw new DBException("Datasource config parse error", ex);
                }
            }
        } catch (DBException ex) {
            log.warn("Can't load profiles config from " + CONFIG_FILE_NAME, ex);
        }
    }


    private synchronized void saveProfiles() {
        if (customProfiles == null) {
            return;
        }
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            log.warn("The user has no permission to save data formatter configuration");
            return;
        }
        try (StringWriter out = new StringWriter()) {
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("profiles");
            for (DBDDataFormatterProfile profile : customProfiles) {
                xml.startElement("profile");
                xml.addAttribute("name", profile.getProfileName());
                SimplePreferenceStore store = (SimplePreferenceStore) profile.getPreferenceStore();
                Map<String, String> props = store.getProperties();
                if (props != null) {
                    for (Map.Entry<String, String> entry : props.entrySet()) {
                        xml.startElement("property");
                        xml.addAttribute("name", entry.getKey());
                        xml.addAttribute("value", entry.getValue());
                        xml.endElement();
                    }
                }
                xml.endElement();
            }
            xml.endElement();
            xml.flush();

            out.flush();
            DBWorkbench.getPlatform().getProductConfigurationController()
                .saveConfigurationFile(CONFIG_FILE_NAME, out.getBuffer().toString());
        } catch (Throwable ex) {
            log.warn("Failed to save data formatter profiles to " + CONFIG_FILE_NAME, ex);
        }
    }

    /**
     * Create custom data formatter profile with specified name and default settings
     */
    public synchronized DBDDataFormatterProfile createCustomProfile(String profileName) {
        getCustomProfilesInternal();
        DBDDataFormatterProfile profile = new DataFormatterProfile(profileName, new CustomProfileStore());
        customProfiles.add(profile);
        saveProfiles();
        return profile;
    }

    /**
     * Delete custom data formatter profile
     */
    public synchronized void deleteCustomProfile(DBDDataFormatterProfile profile) {
        getCustomProfilesInternal();
        if (customProfiles.remove(profile)) {
            saveProfiles();
        }
    }

    private class CustomProfileStore extends SimplePreferenceStore {
        private CustomProfileStore()
        {
            super(DBWorkbench.getPlatform().getPreferenceStore());
        }

        @Override
        public void save() throws IOException
        {
            saveProfiles();
        }
    }

    private class FormattersParser extends SAXListener.BaseListener
    {
        private String profileName;
        private SimplePreferenceStore curStore;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals("profile")) {
                curStore = new CustomProfileStore();
                profileName = atts.getValue("name");
            } else if (localName.equals("property")) {
                if (curStore != null) {
                    curStore.setValue(
                        atts.getValue("name"),
                        atts.getValue("value"));
                }
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals("profile")) {
                if (!CommonUtils.isEmpty(profileName)) {
                    DataFormatterProfile profile = new DataFormatterProfile(profileName, curStore);
                    customProfiles.add(profile);
                }
            }
        }
    }

}
