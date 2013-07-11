/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.runtime.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SQLQueryParameterRegistry
{
    static final Log log = LogFactory.getLog(SQLQueryParameterRegistry.class);

    public static final String CONFIG_FILE_NAME = "parameter-bindings.xml"; //$NON-NLS-1$
    public static final String TAG_PARAMETER = "parameter";

    private static SQLQueryParameterRegistry registry;
    private final Map<String, String> parameterMap = new HashMap<String, String>();

    private SQLQueryParameterRegistry()
    {
    }

    public static synchronized SQLQueryParameterRegistry getInstance()
    {
        if (registry == null) {
            registry = new SQLQueryParameterRegistry();
            registry.loadProfiles();
        }
        return registry;
    }

    public String getParameter(String name)
    {
        return parameterMap.get(name.toUpperCase());
    }

    public void setParameter(String name, String value)
    {
        parameterMap.put(name.toUpperCase(), value);
    }

    private void loadProfiles()
    {
        File storeFile = DBeaverCore.getInstance().getConfigurationFile(CONFIG_FILE_NAME, true);
        if (!storeFile.exists()) {
            return;
        }
        try {
            InputStream is = new FileInputStream(storeFile);
            try {
                try {
                    SAXReader parser = new SAXReader(is);
                    try {
                        parser.parse(new ParametersParser());
                    }
                    catch (XMLException ex) {
                        throw new DBException("Parameters binding parse error", ex);
                    }
                } catch (DBException ex) {
                    log.warn("Can't load parameters binding from " + storeFile.getPath(), ex);
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

    public void save()
    {
        File storeFile = DBeaverCore.getInstance().getConfigurationFile(CONFIG_FILE_NAME, false);
        try {
            OutputStream os = new FileOutputStream(storeFile);
            try {
                XMLBuilder xml = new XMLBuilder(os, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
                xml.setButify(true);
                xml.startElement("bindings");
                for (Map.Entry<String, String> binding : parameterMap.entrySet()) {
                    xml.startElement(TAG_PARAMETER);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, binding.getKey());
                    xml.addText(binding.getValue());
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
            log.warn("Can't open parameters binding file " + storeFile.getPath(), ex);
        }
    }

    private class ParametersParser implements SAXListener
    {
        private String curParameterName;
        private StringBuilder curParameterValue = new StringBuilder();

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(TAG_PARAMETER)) {
                curParameterName = atts.getValue(RegistryConstants.ATTR_NAME);
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
            if (curParameterName != null) {
                curParameterValue.append(data);
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals(TAG_PARAMETER) && curParameterName != null) {
                parameterMap.put(curParameterName.toUpperCase(), curParameterValue.toString());
                curParameterName = null;
                curParameterValue.setLength(0);
            }
        }
    }

}
