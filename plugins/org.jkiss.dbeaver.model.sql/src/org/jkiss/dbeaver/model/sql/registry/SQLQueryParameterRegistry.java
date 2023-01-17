/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.registry;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SQLQueryParameterRegistry {
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

    private SQLQueryParameterRegistry() {
    }

    public static synchronized SQLQueryParameterRegistry getInstance() {
        if (registry == null) {
            registry = new SQLQueryParameterRegistry();
            registry.loadParameters();
        }
        return registry;
    }

    public List<ParameterInfo> getAllParameters() {
        return new ArrayList<>(parameterMap.values());
    }

    public ParameterInfo getParameter(String name) {
        return parameterMap.get(name);
    }

    public void setParameter(String name, String value) {
        parameterMap.put(name, new ParameterInfo(name, value));
    }

    public void deleteParameter(String name) {
        parameterMap.remove(name);
    }

    private void loadParameters() {
        Path storeFile = DBWorkbench.getPlatform().getLocalConfigurationFile(CONFIG_FILE_NAME);
        if (!Files.exists(storeFile)) {
            return;
        }
        try (InputStream is = Files.newInputStream(storeFile)) {
            SAXReader parser = new SAXReader(is);
            try {
                parser.parse(new ParametersParser());
            } catch (XMLException ex) {
                throw new DBException("Parameters binding parse error", ex);
            }
        } catch (DBException ex) {
            log.warn("Can't load parameters binding from " + storeFile, ex);
        } catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }

    public void save() {
        Path storeFile = DBWorkbench.getPlatform().getLocalConfigurationFile(CONFIG_FILE_NAME);
        try (OutputStream os = Files.newOutputStream(storeFile)) {
            XMLBuilder xml = new XMLBuilder(os, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("bindings");
            for (ParameterInfo param : parameterMap.values()) {
                xml.startElement(TAG_PARAMETER);
                xml.addAttribute("name", param.name);
                xml.addAttribute("value", param.value);
                xml.endElement();
            }
            xml.endElement();
            xml.flush();
        } catch (IOException ex) {
            log.warn("IO error", ex);
        }
    }

    private class ParametersParser implements SAXListener {
        private String curParameterName, curParameterValue;
        private StringBuilder legacyParameterValue = new StringBuilder();

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException {
            if (localName.equals(TAG_PARAMETER)) {
                curParameterName = atts.getValue("name");
                curParameterValue = atts.getValue("value");
            }
        }

        @Override
        public void saxText(SAXReader reader, String data)
            throws XMLException {
            if (curParameterName != null) {
                legacyParameterValue.append(data);
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName)
            throws XMLException {
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
                    curParameterName,
                    new ParameterInfo(curParameterName, curParameterValue));
                curParameterName = null;
                legacyParameterValue.setLength(0);
            }
        }
    }

}
