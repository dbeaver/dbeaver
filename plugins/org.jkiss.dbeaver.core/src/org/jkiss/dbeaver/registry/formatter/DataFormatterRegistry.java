/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.formatter;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFormatterRegistry
{
    static final Log log = Log.getLog(DataFormatterRegistry.class);

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

    public synchronized DBDDataFormatterProfile getGlobalProfile()
    {
        if (globalProfile == null) {
            globalProfile = new DataFormatterProfile(
                "Global",
                DBeaverCore.getGlobalPreferenceStore());
        }
        return globalProfile;
    }

    @Nullable
    public DBDDataFormatterProfile getCustomProfile(String name)
    {
        for (DBDDataFormatterProfile profile : getCustomProfiles()) {
            if (profile.getProfileName().equals(name)) {
                return profile;
            }
        }
        return null;
    }

    public synchronized List<DBDDataFormatterProfile> getCustomProfiles()
    {
        if (customProfiles == null) {
            loadProfiles();
        }
        return customProfiles;
    }

    private void loadProfiles()
    {
        customProfiles = new ArrayList<>();

        File storeFile = DBeaverCore.getInstance().getConfigurationFile(CONFIG_FILE_NAME, true);
        if (!storeFile.exists()) {
            return;
        }
        try {
            try (InputStream is = new FileInputStream(storeFile)) {
                SAXReader parser = new SAXReader(is);
                try {
                    parser.parse(new FormattersParser());
                } catch (XMLException ex) {
                    throw new DBException("Datasource config parse error", ex);
                }
            } catch (DBException ex) {
                log.warn("Can't load profiles config from " + storeFile.getPath(), ex);
            }
        }
        catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }


    private void saveProfiles()
    {
        if (customProfiles == null) {
            return;
        }
        File storeFile = DBeaverCore.getInstance().getConfigurationFile(CONFIG_FILE_NAME, false);
        try (OutputStream os = new FileOutputStream(storeFile)) {
            XMLBuilder xml = new XMLBuilder(os, GeneralUtils.DEFAULT_FILE_CHARSET_NAME);
            xml.setButify(true);
            xml.startElement(RegistryConstants.TAG_PROFILES);
            for (DBDDataFormatterProfile profile : customProfiles) {
                xml.startElement(RegistryConstants.TAG_PROFILE);
                xml.addAttribute(RegistryConstants.ATTR_NAME, profile.getProfileName());
                SimplePreferenceStore store = (SimplePreferenceStore) profile.getPreferenceStore();
                Map<String, String> props = store.getProperties();
                if (props != null) {
                    for (Map.Entry<String,String> entry : props.entrySet()) {
                        xml.startElement(RegistryConstants.TAG_PROPERTY);
                        xml.addAttribute(RegistryConstants.ATTR_NAME, entry.getKey());
                        xml.addAttribute(RegistryConstants.ATTR_VALUE, entry.getValue());
                        xml.endElement();
                    }
                }
                xml.endElement();
            }
            xml.endElement();
            xml.flush();
        }
        catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }

    public DBDDataFormatterProfile createCustomProfile(String profileName)
    {
        getCustomProfiles();
        DBDDataFormatterProfile profile = new DataFormatterProfile(profileName, new CustomProfileStore());
        customProfiles.add(profile);
        saveProfiles();
        return profile;
    }

    public void deleteCustomProfile(DBDDataFormatterProfile profile)
    {
        getCustomProfiles();
        if (customProfiles.remove(profile)) {
            saveProfiles();
        }
    }

    private class CustomProfileStore extends SimplePreferenceStore {
        private CustomProfileStore()
        {
            super(DBeaverCore.getGlobalPreferenceStore());
        }

        @Override
        public void save() throws IOException
        {
            saveProfiles();
        }
    }

    private class FormattersParser implements SAXListener
    {
        private String profileName;
        private SimplePreferenceStore curStore;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(RegistryConstants.TAG_PROFILE)) {
                curStore = new CustomProfileStore();
                profileName = atts.getValue(RegistryConstants.ATTR_NAME);
            } else if (localName.equals(RegistryConstants.TAG_PROPERTY)) {
                if (curStore != null) {
                    curStore.setValue(
                        atts.getValue(RegistryConstants.ATTR_NAME),
                        atts.getValue(RegistryConstants.ATTR_VALUE));
                }
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals(RegistryConstants.TAG_PROFILE)) {
                if (!CommonUtils.isEmpty(profileName)) {
                    DataFormatterProfile profile = new DataFormatterProfile(profileName, curStore);
                    customProfiles.add(profile);
                }
            }
        }
    }

}
