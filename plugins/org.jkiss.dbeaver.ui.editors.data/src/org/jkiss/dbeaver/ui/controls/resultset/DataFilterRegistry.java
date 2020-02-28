/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraintBase;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Viewer columns registry
 */
class DataFilterRegistry {

    private static final Log log = Log.getLog(DataFilterRegistry.class);
    private static final String CONFIG_FILE = "saved-data-filter.xml";

    private static final String OBJ_PATH_DELIMITER = "@@/@@";

    private static DataFilterRegistry instance;

    public static synchronized DataFilterRegistry getInstance() {
        if (instance == null) {
            instance = new DataFilterRegistry();
        }
        return instance;
    }

    public static class SavedDataFilter {

        private Map<String, DBDAttributeConstraintBase> constraints = new LinkedHashMap<>();
        private boolean anyConstraint; // means OR condition
        private String order;
        private String where;

        SavedDataFilter() {

        }

        SavedDataFilter(DBPDataSource dataSource, DBDDataFilter dataFilter) {
            for (DBDAttributeConstraint c : dataFilter.getConstraints()) {
                constraints.put(DBUtils.getQuotedIdentifier(dataSource, c.getAttribute().getName()), new DBDAttributeConstraint(c));
            }
            this.anyConstraint = dataFilter.isAnyConstraint();
            this.order = dataFilter.getOrder();
            this.where = dataFilter.getWhere();
        }

        void restoreDataFilter(DBRProgressMonitor monitor, DBSDataContainer dataContainer, DBDDataFilter dataFilter) throws DBException {
            dataFilter.setAnyConstraint(anyConstraint);
            dataFilter.setOrder(this.order);
            dataFilter.setWhere(this.where);
            for (Map.Entry<String, DBDAttributeConstraintBase> savedC : constraints.entrySet()) {
                String attrName = savedC.getKey();
                DBDAttributeConstraint attrC = dataFilter.getConstraint(attrName);
                if (attrC == null) {
                    if (dataContainer instanceof DBSEntity) {
                        DBSEntityAttribute attribute = ((DBSEntity) dataContainer).getAttribute(monitor, attrName);
                        if (attribute != null) {
                            attrC = new DBDAttributeConstraint(attribute, attribute.getOrdinalPosition());
                            dataFilter.addConstraints(Collections.singletonList(attrC));
                        }
                    }
                }
                if (attrC != null) {
                    attrC.copyFrom(savedC.getValue());
                }
            }
        }
    }

    private final Map<String, SavedDataFilter> savedFilters = new HashMap<>();
    private volatile ConfigSaver saver = null;

    public DataFilterRegistry() {
        File columnsConfig = DBWorkbench.getPlatform().getConfigurationFile(CONFIG_FILE);
        if (columnsConfig.exists()) {
            loadConfiguration(columnsConfig);
        }
    }

    SavedDataFilter getSavedConfig(DBSDataContainer object) {
        String objectId = makeObjectId(object);
        synchronized (savedFilters) {
            return savedFilters.get(objectId);
        }
    }

    void saveDataFilter(DBSDataContainer object, DBDDataFilter dataFilter) {
        String objectId = makeObjectId(object);
        synchronized (savedFilters) {
            if (dataFilter.hasFilters()) {
                SavedDataFilter newStates = new SavedDataFilter(object.getDataSource(), dataFilter);
                savedFilters.put(objectId, newStates);
            } else {
                savedFilters.remove(objectId);
            }

            if (saver == null) {
                saver = new ConfigSaver();
                saver.schedule(3000);
            }
        }
    }

    public static String makeObjectId(DBSObject object) {
        DBSObject[] path = DBUtils.getObjectPath(object, true);
        StringBuilder objName = new StringBuilder();
        for (DBSObject p : path) {
            if (objName.length() > 0) objName.append(OBJ_PATH_DELIMITER);
            objName.append(p.getName());
        }
        return objName.toString();
    }

    private void loadConfiguration(File configFile) {
        savedFilters.clear();
        try (InputStream in = new FileInputStream(configFile)) {
            SAXReader parser = new SAXReader(in);
            final DataFilterParser dsp = new DataFilterParser();
            parser.parse(dsp);
        } catch (Exception e) {
            log.error("Error loading data filters config", e);
        }

    }

    private class ConfigSaver extends AbstractJob {
        protected ConfigSaver() {
            super("Data filters config save");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (savedFilters) {
                //log.debug("Save column config " + System.currentTimeMillis());
                flushConfig();
                saver = null;
            }
            return Status.OK_STATUS;
        }

        private void flushConfig() {

            File configFile = DBWorkbench.getPlatform().getConfigurationFile(CONFIG_FILE);
            try (OutputStream out = new FileOutputStream(configFile)) {
                XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
                xml.setButify(true);
                try (final XMLBuilder.Element e = xml.startElement("data-filters")) {
                    for (Map.Entry<String, SavedDataFilter> entry : savedFilters.entrySet()) {
                        try (final XMLBuilder.Element e2 = xml.startElement("filter")) {
                            xml.addAttribute("objectId", entry.getKey());
                            SavedDataFilter sdf = entry.getValue();
                            xml.addAttribute("anyConstraint", sdf.anyConstraint);
                            if (!CommonUtils.isEmpty(sdf.order)) xml.addAttribute("order", sdf.order);
                            if (!CommonUtils.isEmpty(sdf.where)) xml.addAttribute("where", sdf.where);
                            for (Map.Entry<String, DBDAttributeConstraintBase> attrCE : sdf.constraints.entrySet()) {
                                try (final XMLBuilder.Element e3 = xml.startElement("constraint")) {
                                    xml.addAttribute("name", attrCE.getKey());
                                    DBDAttributeConstraintBase attrC = attrCE.getValue();
                                    if (!attrC.isVisible()) {
                                        xml.addAttribute("visible", false);
                                    }
                                    xml.addAttribute("pos", attrC.getVisualPosition());
                                    xml.addAttribute("order", attrC.getOrderPosition());
                                    if (!CommonUtils.isEmpty(attrC.getCriteria())) {
                                        xml.addAttribute("criteria", attrC.getCriteria());
                                    }
                                    if (attrC.getOperator() != null) {
                                        xml.addAttribute("operator", attrC.getOperator().name());
                                    }
                                    if (!CommonUtils.isEmpty(attrC.getEntityAlias())) {
                                        xml.addAttribute("entity", attrC.getEntityAlias());
                                    }
                                    if (attrC.getValue() != null) {
                                        xml.startElement("value");
                                        xml.addText(GeneralUtils.serializeObject(attrC.getValue()));
                                        xml.endElement();
                                    }
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

    private class DataFilterParser extends SAXListener.BaseListener {

        private SavedDataFilter curSavedDataFilter = null;
        private DBDAttributeConstraintBase curSavedConstraint = null;
        private boolean isInValue = false;

        private DataFilterParser() {
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
            switch (localName) {
                case "filter": {
                    curSavedDataFilter = new SavedDataFilter();
                    curSavedDataFilter.anyConstraint = CommonUtils.toBoolean(atts.getValue("anyConstraint"));
                    String objectId = atts.getValue("objectId");
                    savedFilters.put(objectId, curSavedDataFilter);
                    break;
                }
                case "constraint": {
                    if (curSavedDataFilter != null) {
                        curSavedConstraint = new DBDAttributeConstraintBase();
                        String name = atts.getValue("name");
                        curSavedConstraint.setVisualPosition(CommonUtils.toInt(atts.getValue("pos")));
                        curSavedConstraint.setOrderPosition(CommonUtils.toInt(atts.getValue("order")));
                        curSavedConstraint.setCriteria(atts.getValue("criteria"));
                        curSavedConstraint.setVisible(CommonUtils.getBoolean(atts.getValue("visible"), true));
                        String operName = atts.getValue("operator");
                        if (!CommonUtils.isEmpty(operName)) {
                            try {
                                curSavedConstraint.setOperator(DBCLogicalOperator.valueOf(operName));
                            } catch (IllegalArgumentException e) {
                                log.error(e);
                            }
                        }
                        curSavedConstraint.setEntityAlias(atts.getValue("entity"));
                        curSavedDataFilter.constraints.put(name, curSavedConstraint);
                    }
                    break;
                }
                case "value": {
                    if (curSavedConstraint != null) {
                        isInValue = true;
                    }
                    break;
                }
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
            switch (localName) {
                case "filter": {
                    curSavedDataFilter = null;
                    break;
                }
                case "constraint": {
                    curSavedConstraint = null;
                    break;
                }
                case "value": {
                    isInValue = false;
                    break;
                }
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) throws XMLException {
            if (isInValue) {
                curSavedConstraint.setValue(GeneralUtils.deserializeObject(data));
            }
        }
    }
}