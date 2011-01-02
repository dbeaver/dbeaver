/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.xml.SAXListener;
import net.sf.jkiss.utils.xml.SAXReader;
import net.sf.jkiss.utils.xml.XMLBuilder;
import net.sf.jkiss.utils.xml.XMLException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFormatterRegistry
{
    static final Log log = LogFactory.getLog(DataFormatterRegistry.class);

    public static final String CONFIG_FILE_NAME = "dataformat-profiles.xml";

    private final List<DataFormatterDescriptor> dataFormatterList = new ArrayList<DataFormatterDescriptor>();
    private final Map<String, DataFormatterDescriptor> dataFormatterMap = new HashMap<String, DataFormatterDescriptor>();
    private DBDDataFormatterProfile globalProfile;
    private List<DBDDataFormatterProfile> customProfiles = null;

    public DataFormatterRegistry(IExtensionRegistry registry)
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
            globalProfile = new DataFormatterProfile("Global", DBeaverCore.getInstance().getGlobalPreferenceStore());
        }
        return globalProfile;
    }

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
        customProfiles = new ArrayList<DBDDataFormatterProfile>();

        File storeFile = new File(DBeaverCore.getInstance().getRootPath().toFile(), CONFIG_FILE_NAME);
        if (!storeFile.exists()) {
            return;
        }
        try {
            InputStream is = new FileInputStream(storeFile);
            try {
                try {
                    SAXReader parser = new SAXReader(is);
                    try {
                        parser.parse(new FormattersParser());
                    }
                    catch (XMLException ex) {
                        throw new DBException("Datasource config parse error", ex);
                    }
                } catch (DBException ex) {
                    log.warn("Can't load profiles config from " + storeFile.getPath(), ex);
                }
                finally {
                    is.close();
                }
            }
            catch (IOException ex) {
                log.warn("IO error", ex);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Can't open config file " + storeFile.getAbsolutePath(), ex);
        }
    }


    private void saveProfiles()
    {
        if (customProfiles == null) {
            return;
        }
        File storeFile = new File(DBeaverCore.getInstance().getRootPath().toFile(), CONFIG_FILE_NAME);
        try {
            OutputStream os = new FileOutputStream(storeFile);
            try {
                XMLBuilder xml = new XMLBuilder(os, ContentUtils.DEFAULT_FILE_CHARSET);
                xml.setButify(true);
                xml.startElement("profiles");
                for (DBDDataFormatterProfile profile : customProfiles) {
                    xml.startElement("profile");
                    xml.addAttribute("name", profile.getProfileName());
                    AbstractPreferenceStore store = (AbstractPreferenceStore) profile.getPreferenceStore();
                    Map<String, String> props = store.getProperties();
                    if (props != null) {
                        for (Map.Entry<String,String> entry : props.entrySet()) {
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
                os.close();
            }
            catch (IOException ex) {
                log.warn("IO error", ex);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Can't open profiles store file " + storeFile.getPath(), ex);
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

    private class CustomProfileStore extends AbstractPreferenceStore {
        private CustomProfileStore()
        {
            super(DBeaverCore.getInstance().getGlobalPreferenceStore());
        }

        public void save() throws IOException
        {
            saveProfiles();
        }
    }

    private class FormattersParser implements SAXListener
    {
        private String profileName;
        private AbstractPreferenceStore curStore;

        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals("profile")) {
                curStore = new CustomProfileStore();
                profileName = atts.getValue("name");
            } else if (localName.equals("property")) {
                if (curStore != null) {
                    curStore.setValue(atts.getValue("name"), atts.getValue("value"));
                }
            }
        }

        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
        }

        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals("profile")) {
                DataFormatterProfile profile = new DataFormatterProfile(profileName, curStore);
                customProfiles.add(profile);
            }
        }
    }

}
