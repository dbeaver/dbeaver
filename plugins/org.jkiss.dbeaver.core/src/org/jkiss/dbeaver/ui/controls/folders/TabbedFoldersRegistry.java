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
package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.*;

/**
 * Viewer columns registry
 */
class TabbedFoldersRegistry {

    private static final Log log = Log.getLog(TabbedFoldersRegistry.class);
    private static final String COLUMNS_CONFIG_FILE = "tabs_settings.xml";

    private static TabbedFoldersRegistry instance;

    public static synchronized TabbedFoldersRegistry getInstance() {
        if (instance == null) {
            instance = new TabbedFoldersRegistry();
        }
        return instance;
    }

    private final Map<String, TabbedFolderState> savedStates = new HashMap<>();
    private volatile ConfigSaver saver = null;

    public TabbedFoldersRegistry() {
        File savedStates = DBeaverActivator.getConfigurationFile(COLUMNS_CONFIG_FILE);
        if (savedStates.exists()) {
            loadConfiguration(savedStates);
        }
    }

    @NotNull
    TabbedFolderState getFolderState(String objectId) {
        synchronized (savedStates) {
            TabbedFolderState folderState = savedStates.get(objectId);
            if (folderState == null) {
                folderState = new TabbedFolderState();
                savedStates.put(objectId, folderState);
            }
            return folderState;
        }
    }

    void saveConfig() {
        synchronized (savedStates) {
            if (saver == null) {
                saver = new ConfigSaver();
                saver.schedule(3000);
            }
        }
    }

    private void loadConfiguration(File configFile) {
        savedStates.clear();
        try (InputStream in = new FileInputStream(configFile)) {
            SAXReader parser = new SAXReader(in);
            final FolderStateParser dsp = new FolderStateParser();
            parser.parse(dsp);
        } catch (Exception e) {
            log.error("Error loading columns configuration", e);
        }

    }

    private class ConfigSaver extends AbstractJob {
        ConfigSaver() {
            super("Tab folders configuration save");
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (savedStates) {
                //log.debug("Save column config " + System.currentTimeMillis());
                flushConfig();
                saver = null;
            }
            return Status.OK_STATUS;
        }

        private void flushConfig() {

            File configFile = DBeaverActivator.getConfigurationFile(COLUMNS_CONFIG_FILE);
            try (OutputStream out = new FileOutputStream(configFile)) {
                XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
                xml.setButify(true);
                try (final XMLBuilder.Element e = xml.startElement("folders")) {
                    for (Map.Entry<String, TabbedFolderState> entry : savedStates.entrySet()) {
                        try (final XMLBuilder.Element e2 = xml.startElement("folder")) {
                            xml.addAttribute("id", entry.getKey());
                            for (Map.Entry<String, TabbedFolderState.TabState> tab : entry.getValue().getTabStates().entrySet()) {
                                try (final XMLBuilder.Element e3 = xml.startElement("tab")) {
                                    xml.addAttribute("id", tab.getKey());
                                    xml.addAttribute("height", tab.getValue().height);
                                    xml.addAttribute("width", tab.getValue().width);
                                    xml.addAttribute("embedded", tab.getValue().embedded);
                                }
                            }
                        }
                    }
                }
                xml.flush();
            } catch (Exception e) {
                log.error("Error saving tabs configuration", e);
            }
        }

    }

    private class FolderStateParser extends SAXListener.BaseListener {

        private TabbedFolderState curTabbedFolderState = null;

        private FolderStateParser() {
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
            switch (localName) {
                case "folders":
                    break;
                case "folder":
                    curTabbedFolderState = new TabbedFolderState();
                    savedStates.put(atts.getValue("id"), curTabbedFolderState);
                    break;
                case "tab":
                    if (curTabbedFolderState != null) {
                        TabbedFolderState.TabState tabState = new TabbedFolderState.TabState();
                        tabState.height = CommonUtils.toInt(atts.getValue("height"), 0);
                        tabState.width = CommonUtils.toInt(atts.getValue("width"), 0);
                        tabState.embedded = CommonUtils.toBoolean(atts.getValue("embedded"));
                        curTabbedFolderState.setTabState(atts.getValue("id"), tabState);
                    }
                    break;
            }
        }

    }
}