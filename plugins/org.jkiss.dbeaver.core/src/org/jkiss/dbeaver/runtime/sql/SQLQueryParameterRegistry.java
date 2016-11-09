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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
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
        public String value;

        public ParameterInfo(String name, String value) {
            this.name = name;
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

    public void setParameter(String name, String value)
    {
        parameterMap.put(name.toUpperCase(Locale.ENGLISH), new ParameterInfo(name, value));
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
            XMLBuilder xml = new XMLBuilder(os, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("bindings");
            for (ParameterInfo param : parameterMap.values()) {
                xml.startElement(TAG_PARAMETER);
                xml.addAttribute(RegistryConstants.ATTR_NAME, param.name);
                xml.addAttribute(RegistryConstants.ATTR_VALUE, param.value);
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
        private String curParameterName, curParameterValue;
        private StringBuilder legacyParameterValue = new StringBuilder();

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(TAG_PARAMETER)) {
                curParameterName = atts.getValue(RegistryConstants.ATTR_NAME);
                curParameterValue = atts.getValue(RegistryConstants.ATTR_VALUE);
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException
        {
            if (curParameterName != null) {
                legacyParameterValue.append(data);
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException
        {
            if (localName.equals(TAG_PARAMETER) && curParameterName != null) {
                if (curParameterValue == null) {
                    String legacyValue = legacyParameterValue.toString().trim();
                    if (!legacyValue.isEmpty()) {
                        if (Character.isLetter(legacyValue.charAt(0))) {
                            // Quote strings
                            legacyValue = "'" + legacyValue + "'";
                        }
                    }
                    curParameterValue = legacyValue;
                }
                parameterMap.put(
                    curParameterName.toUpperCase(),
                    new ParameterInfo(curParameterName, curParameterValue));
                curParameterName = null;
                legacyParameterValue.setLength(0);
            }
        }
    }

}
