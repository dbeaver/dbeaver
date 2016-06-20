/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SQLQueryParameterRegistry
{
    private static final Log log = Log.getLog(SQLQueryParameterRegistry.class);

    public static final String CONFIG_FILE_NAME = "parameter-bindings.xml"; //$NON-NLS-1$
    public static final String TAG_PARAMETER = "parameter";

    private static SQLQueryParameterRegistry registry;
    private final Map<String, ParameterInfo> parameterMap = new LinkedHashMap<>();

    public static class ParameterInfo {
        public String name;
        public String type;
        public String value;

        public ParameterInfo(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

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

    public ParameterInfo getParameter(String name)
    {
        return parameterMap.get(name.toUpperCase(Locale.ENGLISH));
    }

    public void setParameter(String name, String type, String value)
    {
        parameterMap.put(name.toUpperCase(Locale.ENGLISH), new ParameterInfo(name, type, value));
    }

    private void loadProfiles()
    {
        File storeFile = DBeaverActivator.getConfigurationFile(CONFIG_FILE_NAME);
        if (!storeFile.exists()) {
            return;
        }
        try (InputStream is = new FileInputStream(storeFile)) {
            SAXReader parser = new SAXReader(is);
            try {
                parser.parse(new ParametersParser());
            } catch (XMLException ex) {
                throw new DBException("Parameters binding parse error", ex);
            }
        } catch (DBException ex) {
            log.warn("Can't load parameters binding from " + storeFile.getPath(), ex);
        } catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }

    public void save()
    {
        File storeFile = DBeaverActivator.getConfigurationFile(CONFIG_FILE_NAME);
        try (OutputStream os = new FileOutputStream(storeFile)) {
            XMLBuilder xml = new XMLBuilder(os, GeneralUtils.DEFAULT_FILE_CHARSET_NAME);
            xml.setButify(true);
            xml.startElement("bindings");
            for (Map.Entry<String, ParameterInfo> binding : parameterMap.entrySet()) {
                xml.startElement(TAG_PARAMETER);
                xml.addAttribute(RegistryConstants.ATTR_NAME, binding.getValue().name);
                if (binding.getValue().type != null) {
                    xml.addAttribute(RegistryConstants.ATTR_TYPE, binding.getValue().type);
                }
                xml.addText(binding.getValue().value);
                xml.endElement();
            }
            xml.endElement();
            xml.flush();
        }
        catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }

    private class ParametersParser implements SAXListener
    {
        private String curParameterName;
        private String curParameterType;
        private StringBuilder curParameterValue = new StringBuilder();

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(TAG_PARAMETER)) {
                curParameterName = atts.getValue(RegistryConstants.ATTR_NAME);
                curParameterType = atts.getValue(RegistryConstants.ATTR_TYPE);
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
                parameterMap.put(
                    curParameterName.toUpperCase(),
                    new ParameterInfo(curParameterName, curParameterType, curParameterValue.toString()));
                curParameterName = null;
                curParameterValue.setLength(0);
            }
        }
    }

}
