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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
class ViewerColumnRegistry {

    private static final Log log = Log.getLog(ViewerColumnRegistry.class);
    private static final String COLUMNS_CONFIG_FILE = "column_settings.xml";

    private static ViewerColumnRegistry instance;

    public static synchronized ViewerColumnRegistry getInstance() {
        if (instance == null) {
            instance = new ViewerColumnRegistry();
        }
        return instance;
    }

    public static class ColumnState {
        String name;
        boolean visible;
        int order;
        int width;

        public ColumnState() {
        }

        public ColumnState(ColumnState source) {
            this.name = source.name;
            this.visible = source.visible;
            this.order = source.order;
            this.width = source.width;
        }

        @Override
        public String toString() {
            return name + ":" + order;
        }
    }

    private final Map<String, List<ColumnState>> columnsConfig = new TreeMap<>();
    private volatile ConfigSaver saver = null;

    public ViewerColumnRegistry() {
        File columnsConfig = DBeaverActivator.getConfigurationFile(COLUMNS_CONFIG_FILE);
        if (columnsConfig.exists()) {
            loadConfiguration(columnsConfig);
        }
    }

    Collection<ColumnState> getSavedConfig(String controlId) {
        synchronized (columnsConfig) {
            return columnsConfig.get(controlId);
        }
    }

    void updateConfig(String controlId, Collection<? extends ColumnState> columns) {
        synchronized (columnsConfig) {
            List<ColumnState> newStates = new ArrayList<>(columns.size());
            for (ColumnState state : columns) {
                newStates.add(new ColumnState(state));
            }
            columnsConfig.put(controlId, newStates);

            if (saver == null) {
                saver = new ConfigSaver();
                saver.schedule(3000);
            }
        }
    }

    private void loadConfiguration(File configFile) {
        columnsConfig.clear();
        try (InputStream in = new FileInputStream(configFile)) {
            SAXReader parser = new SAXReader(in);
            final ColumnsParser dsp = new ColumnsParser();
            parser.parse(dsp);
        } catch (Exception e) {
            log.error("Error loading columns configuration", e);
        }

    }

    private class ConfigSaver extends AbstractJob {
        protected ConfigSaver() {
            super("Columns configuration save");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (columnsConfig) {
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
                try (final XMLBuilder.Element e = xml.startElement("items")) {
                    for (Map.Entry<String, List<ColumnState>> entry : columnsConfig.entrySet()) {
                        try (final XMLBuilder.Element e2 = xml.startElement("item")) {
                            xml.addAttribute("id", entry.getKey());
                            for (ColumnState column : entry.getValue()) {
                                try (final XMLBuilder.Element e3 = xml.startElement("column")) {
                                    xml.addAttribute("name", column.name);
                                    xml.addAttribute("visible", column.visible);
                                    xml.addAttribute("order", column.order);
                                    xml.addAttribute("width", column.width);
                                }
                            }
                        }
                    }
                }
                xml.flush();
            } catch (Exception e) {
                log.error("Error saving columns configuration", e);
            }
        }

    }

    private class ColumnsParser implements SAXListener {

        private List<ColumnState> curColumnState = null;

        private ColumnsParser() {
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
            switch (localName) {
                case "items":
                    break;
                case "item":
                    curColumnState = new ArrayList<>();
                    columnsConfig.put(atts.getValue("id"), curColumnState);
                    break;
                case "column":
                    if (curColumnState != null) {
                        ColumnState col = new ColumnState();
                        col.name = atts.getValue("name");
                        col.visible = CommonUtils.getBoolean(atts.getValue("visible"), true);
                        col.order = CommonUtils.toInt(atts.getValue("order"), 0);
                        col.width = CommonUtils.toInt(atts.getValue("width"), 0);
                        curColumnState.add(col);
                    }
                    break;
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) throws XMLException {
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
        }
    }
}