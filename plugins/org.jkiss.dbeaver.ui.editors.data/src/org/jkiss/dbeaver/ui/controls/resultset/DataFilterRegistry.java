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
package org.jkiss.dbeaver.ui.controls.resultset;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraintBase;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

/**
 * Viewer columns registry
 */
class DataFilterRegistry {

    private static final Log log = Log.getLog(DataFilterRegistry.class);
    private static final String CONFIG_FILE = "saved-data-filter.xml";

    private static final String OBJ_PATH_DELIMITER = "@@/@@";

    private static DataFilterRegistry instance;

    @NotNull
    public static synchronized DataFilterRegistry getInstance() {
        if (instance == null) {
            instance = new DataFilterRegistry();
        }
        return instance;
    }
    
    private static class RestoredAttributesInfo {
        final Map<RestoredAttribute, RestoredAttribute> boundAttrs;
        
        private RestoredAttributesInfo(@NotNull Map<RestoredAttribute, RestoredAttribute> boundAttrs) {
            this.boundAttrs = boundAttrs;
        }
        
        @NotNull
        public static RestoredAttributesInfo bindToDataSource(
            @NotNull SavedDataFilter savedFilter,
            @Nullable DBPDataSource dataSource
        ) {
            return new RestoredAttributesInfo(
                dataSource == null ? Collections.emptyMap() : bindToDataSourceImpl(savedFilter, dataSource));
        }
        
        @NotNull
        private static Map<RestoredAttribute, RestoredAttribute> bindToDataSourceImpl(
            @NotNull SavedDataFilter savedFilter,
            @NotNull DBPDataSource dataSource
        ) {
            Map<RestoredAttribute, RestoredAttribute> boundAttrs = new HashMap<>();
            ArrayList<RestoredAttribute> restoredAttributes = new ArrayList<>();

            for (DBDAttributeConstraintBase savedConstraint : savedFilter.constraints.values()) {
                if (savedConstraint instanceof DBDAttributeConstraint) {
                    DBSAttributeBase savedAttr = ((DBDAttributeConstraint) savedConstraint).getAttribute();
                    if (savedAttr instanceof RestoredAttribute) {
                        for (RestoredAttribute attr = (RestoredAttribute) savedAttr;
                            attr != null && !boundAttrs.containsKey(attr);
                            attr = attr.getParentObject()
                        ) {
                            restoredAttributes.add(attr);
                        }
                        while (!restoredAttributes.isEmpty()) {
                            RestoredAttribute attr = restoredAttributes.remove(restoredAttributes.size() - 1);
                            RestoredAttribute boundParent = attr.getParentObject() == null
                                ? null
                                : boundAttrs.get(attr.getParentObject());
                            boundAttrs.put(attr, new RestoredAttribute(boundParent, attr, dataSource));
                        }
                    }
                }
            }
            
            return boundAttrs; 
        }
        
        @NotNull
        public DBDAttributeConstraint restoreOffschemaConstraint(
            @NotNull String unquottedAttrName,
            @NotNull DBDAttributeConstraintBase savedConstraint
        ) {
            DBDAttributeConstraint constraint;
            if (savedConstraint instanceof DBDAttributeConstraint) {
                DBDAttributeConstraint savedAttrConstraint = (DBDAttributeConstraint) savedConstraint;
                if (savedAttrConstraint.getAttribute() instanceof RestoredAttribute) {
                    RestoredAttribute boundAttr = boundAttrs.get(savedAttrConstraint.getAttribute());
                    if (boundAttr != null) {
                        constraint = new DBDAttributeConstraint(boundAttr, savedConstraint.getVisualPosition());
                        constraint.copyFrom(savedConstraint);
                    } else {
                        constraint = savedAttrConstraint;
                    }
                } else {
                    constraint = savedAttrConstraint;
                }
            } else {
                DBPDataKind dataKind = getAttributeValueKind(savedConstraint.getValue());
                DBSAttributeBase attribute = new AbstractAttribute(
                    unquottedAttrName, dataKind.name(), 0, savedConstraint.getVisualPosition(), 0, 0, 0, false, false
                ) {
                    @Override
                    public DBPDataKind getDataKind() {
                        return dataKind;
                    }
                }; 
                constraint = new DBDAttributeConstraint(attribute, savedConstraint.getVisualPosition());
                constraint.copyFrom(savedConstraint);
            }
            return constraint;
        }
    
        @NotNull
        private static DBPDataKind getAttributeValueKind(@NotNull Object value) {
            if (value instanceof String) {
                return DBPDataKind.STRING;
            } else if (value instanceof Number) {
                return DBPDataKind.NUMERIC;
            } else if (value instanceof Boolean) {
                return DBPDataKind.BOOLEAN;
            } else if (value instanceof Date) {
                return DBPDataKind.DATETIME;
            } else if (value instanceof JsonObject) {
                return DBPDataKind.STRUCT;
            } else if (value instanceof JsonArray) {
                return DBPDataKind.ARRAY;
            } else if (value != null && value.getClass().isArray() && Array.getLength(value) > 0) {
                return getAttributeValueKind(Array.get(value, 0));
            } else {
                return DBPDataKind.OBJECT;
            }
        }
    }

    static class SavedDataFilter {

        private Map<String, DBDAttributeConstraintBase> constraints = new LinkedHashMap<>();
        private boolean anyConstraint; // means OR condition
        private String order;
        private String where;

        SavedDataFilter() {

        }

        SavedDataFilter(@NotNull DBPDataSource dataSource, @NotNull DBDDataFilter dataFilter) {
            for (DBDAttributeConstraint c : dataFilter.getConstraints()) {
                if (c.getAttribute() != null) {
                    constraints.put(DBUtils.getQuotedIdentifier(
                        dataSource, c.getAttribute().getName()), new DBDAttributeConstraint(c));
                }
            }
            this.anyConstraint = dataFilter.isAnyConstraint();
            this.order = dataFilter.getOrder();
            this.where = dataFilter.getWhere();
        }

        void restoreDataFilter(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBSDataContainer dataContainer,
            @NotNull DBDDataFilter dataFilter
        ) throws DBException {
            dataFilter.setAnyConstraint(anyConstraint);
            dataFilter.setOrder(this.order);
            dataFilter.setWhere(this.where);
            List<DBDAttributeConstraint> offschemaConstraints = null;
            RestoredAttributesInfo restoredAttrsInfo = RestoredAttributesInfo.bindToDataSource(
                    this, dataContainer.getDataSource()
            );
            for (Map.Entry<String, DBDAttributeConstraintBase> savedC : constraints.entrySet()) {
                String attrName = savedC.getKey();
                DBDAttributeConstraintBase savedConstraint = savedC.getValue();
                if (dataContainer.getDataSource() != null) {
                    attrName = DBUtils.getUnQuotedIdentifier(dataContainer.getDataSource(), attrName);
                }
                DBDAttributeConstraint attrC = dataFilter.getConstraint(attrName);
                if (attrC == null && dataContainer instanceof DBSEntity) {
                    DBSEntityAttribute attribute = ((DBSEntity) dataContainer).getAttribute(monitor, attrName); 
                    if (attribute != null) {
                        attrC = new DBDAttributeConstraint(attribute, attribute.getOrdinalPosition());
                    } else if (savedConstraint != null) {
                        attrC = restoredAttrsInfo.restoreOffschemaConstraint(attrName, savedConstraint);
                    }
                    if (attrC != null) {
                        dataFilter.addConstraints(Collections.singletonList(attrC));
                    }
                }
                if (attrC != null) {
                    attrC.copyFrom(savedConstraint);
                } else if (savedConstraint != null) {
                    if (offschemaConstraints == null) {
                        offschemaConstraints = new ArrayList<>();
                    }
                    offschemaConstraints.add(restoredAttrsInfo.restoreOffschemaConstraint(attrName, savedConstraint));
                }
            }
            if (offschemaConstraints != null && dataContainer.getDataSource() != null) {
                StringBuilder sb = new StringBuilder();
                SQLUtils.appendConditionString(
                    dataFilter, offschemaConstraints, dataContainer.getDataSource(), null, sb, true, false);
                dataFilter.setWhere(sb.toString());
            }
        }
    }

    @NotNull
    private final Map<String, SavedDataFilter> savedFilters = new HashMap<>();
    @Nullable
    private volatile ConfigSaver saver = null;

    public DataFilterRegistry() {
        Path columnsConfig = DBWorkbench.getPlatform().getLocalConfigurationFile(CONFIG_FILE);
        if (Files.exists(columnsConfig)) {
            loadConfiguration(columnsConfig);
        }
    }

    @Nullable
    public SavedDataFilter getSavedConfig(@NotNull DBSDataContainer object) {
        String objectId = makeObjectId(object);
        synchronized (savedFilters) {
            return savedFilters.get(objectId);
        }
    }

    void saveDataFilter(@NotNull DBSDataContainer object, @NotNull DBDDataFilter dataFilter) {
        String objectId = makeObjectId(object);
        synchronized (savedFilters) {
            if (dataFilter.isDirty()) {
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

    @NotNull
    public static String makeObjectId(@NotNull DBSObject object) {
        DBSObject[] path = DBUtils.getObjectPath(object, true);
        StringBuilder objName = new StringBuilder();
        for (DBSObject p : path) {
            if (objName.length() > 0) objName.append(OBJ_PATH_DELIMITER);
            objName.append(p.getName());
        }
        return objName.toString();
    }

    private void loadConfiguration(@NotNull Path configFile) {
        savedFilters.clear();
        try (InputStream in = Files.newInputStream(configFile)) {
            SAXReader parser = new SAXReader(in);
            final DataFilterParser dsp = new DataFilterParser();
            parser.parse(dsp);
        } catch (Exception e) {
            log.error("Error loading data filters config", e);
        }

    }

    private class ConfigSaver extends AbstractJob {
        ConfigSaver() {
            super("Data filters config save");
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            synchronized (savedFilters) {
                //log.debug("Save column config " + System.currentTimeMillis());
                flushConfig();
                saver = null;
            }
            return Status.OK_STATUS;
        }
        
        private Map<DBSAttributeBase, String> collectAttrsInfo(@NotNull XMLBuilder xml, @NotNull SavedDataFilter sdf) throws IOException {
            // we are going to iterate through the collected info in the "root first, children later" order,
            // which is guaranteed by the flattenAttributes(..) implementation,
            // so we'll be able to reestablish child->parent references during deserialization
            Map<DBSAttributeBase, String> attrsInfo = new LinkedHashMap<>();
            for (DBDAttributeConstraintBase attrC : sdf.constraints.values()) {
                if (attrC instanceof DBDAttributeConstraint) {
                    flattenAttributes(attrsInfo, ((DBDAttributeConstraint) attrC).getAttribute());
                }
            }

            if (attrsInfo.size() > 0) {
                try (XMLBuilder.Element e = xml.startElement("flatten-attribute-bindings")) {
                    for (Entry<DBSAttributeBase, String> entry : attrsInfo.entrySet()) {
                        try (XMLBuilder.Element ae = xml.startElement("attribute")) {
                            DBSAttributeBase attribute = entry.getKey();
                            xml.addAttribute("attrEntryId", entry.getValue());
                            if (attribute instanceof DBDAttributeBinding) {
                                DBDAttributeBinding binding = (DBDAttributeBinding) attribute;
                                DBDAttributeBinding parent = binding.getParentObject();
                                if (parent != null) {
                                    xml.addAttribute("parentAttrEntryId", attrsInfo.get(parent));
                                }
                                xml.addAttribute("isPseudoAttribute", binding.isPseudoAttribute());
                            }
                            xml.addAttribute("name", attribute.getName());
                            xml.addAttribute("typeName", attribute.getTypeName());
                            xml.addAttribute("typeId", attribute.getTypeID());
                            xml.addAttribute("dataKind", attribute.getDataKind().name());
                            xml.addAttribute("ordinalPosition", attribute.getOrdinalPosition());
                            xml.addAttribute("maxLength", attribute.getMaxLength());
                            if (attribute.getScale() != null) {
                                xml.addAttribute("scale", attribute.getScale());
                            }
                            if (attribute.getPrecision() != null) {
                                xml.addAttribute("precision", attribute.getPrecision());
                            }
                            xml.addAttribute("isRequired", attribute.isRequired());
                            xml.addAttribute("isAutoGenerated", attribute.isAutoGenerated());
                        }
                    }
                }
            }
            
            return attrsInfo;
        }
        
        private void flattenAttributes(@NotNull Map<DBSAttributeBase, String> attrs, @NotNull DBSAttributeBase attribute) {
            if (!attrs.containsKey(attribute)) {
                if (attribute instanceof DBDAttributeBinding) {
                    DBDAttributeBinding parent = ((DBDAttributeBinding) attribute).getParentObject();
                    if (parent != null) {
                        flattenAttributes(attrs, parent);
                    }
                }
                attrs.put(attribute, "fa" + attrs.size());
            }
        }
        
        private void flushConfig() {
            Path configFile = DBWorkbench.getPlatform().getLocalConfigurationFile(CONFIG_FILE);
            try (OutputStream out = Files.newOutputStream(configFile)) {
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
                            Map<DBSAttributeBase, String> attrsInfo = collectAttrsInfo(xml, sdf);
                            for (Map.Entry<String, DBDAttributeConstraintBase> attrCE : sdf.constraints.entrySet()) {
                                try (final XMLBuilder.Element e3 = xml.startElement("constraint")) {
                                    xml.addAttribute("name", attrCE.getKey());
                                    DBDAttributeConstraintBase attrC = attrCE.getValue();
                                    if (!attrC.isVisible()) {
                                        xml.addAttribute("visible", false);
                                    }
                                    xml.addAttribute("pos", attrC.getVisualPosition());
                                    if (attrC.getOrderPosition() > 0) {
                                        xml.addAttribute("order", attrC.getOrderPosition());
                                        xml.addAttribute("orderDesc", attrC.isOrderDescending());
                                    }
                                    if (!CommonUtils.isEmpty(attrC.getCriteria())) {
                                        xml.addAttribute("criteria", attrC.getCriteria());
                                    }
                                    if (attrC.getOperator() != null) {
                                        xml.addAttribute("operator", attrC.getOperator().name());
                                    }
                                    if (!CommonUtils.isEmpty(attrC.getEntityAlias())) {
                                        xml.addAttribute("entity", attrC.getEntityAlias());
                                    }
                                    if (attrC instanceof DBDAttributeConstraint) {
                                        xml.addAttribute("attrEntryId", attrsInfo.get(((DBDAttributeConstraint) attrC).getAttribute()));
                                    }
                                    if (attrC.getValue() != null) {
                                        xml.startElement("value");
                                        xml.addText(GeneralUtils.serializeObject(attrC.getValue()));
                                        xml.endElement();
                                    }
                                    Object[] options = attrC.getOptions();
                                    if (!ArrayUtils.isEmpty(options)) {
                                        for (int i = 0; i < options.length; i += 2) {
                                            xml.startElement("option");
                                            xml.addAttribute("name", CommonUtils.toString(options[i]));
                                            xml.addText(GeneralUtils.serializeObject(options[i + 1]));
                                            xml.endElement();
                                        }
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

        @Nullable
        private SavedDataFilter curSavedDataFilter = null;
        @Nullable
        private Map<String, RestoredAttribute> attrsInfo = null;
        @Nullable
        private DBDAttributeConstraintBase curSavedConstraint = null;
        private boolean isInValue = false;
        @Nullable
        private String curOptionName = null;

        private DataFilterParser() {
        }

        @Override
        public void saxStartElement(
            @NotNull SAXReader reader,
            @Nullable String namespaceURI,
            @NotNull String localName,
            @NotNull Attributes atts
        ) throws XMLException {
            switch (localName) {
                case "flatten-attribute-bindings": {
                    // we don't actually bother about the order, cause we are just going to resolve attr by name
                    attrsInfo = new LinkedHashMap<>();
                    break;
                }
                case "attribute": {
                    if (attrsInfo != null) {
                        String attrEntryId = atts.getValue("attrEntryId");
                        String parentAttrEntryId = atts.getValue("parentAttrEntryId");

                        RestoredAttribute parent = CommonUtils.isEmpty(parentAttrEntryId) ? null : attrsInfo.get(parentAttrEntryId);
                        
                        attrsInfo.put(attrEntryId, new RestoredAttribute(parent, atts));
                    }
                    break;
                }
                case "filter": {
                    curSavedDataFilter = new SavedDataFilter();
                    curSavedDataFilter.anyConstraint = CommonUtils.toBoolean(atts.getValue("anyConstraint"));
                    curSavedDataFilter.where = atts.getValue("where");
                    curSavedDataFilter.order = atts.getValue("order");
                    String objectId = atts.getValue("objectId");
                    savedFilters.put(objectId, curSavedDataFilter);
                    attrsInfo = null;
                    break;
                }
                case "constraint": {
                    if (curSavedDataFilter != null) {
                        curSavedConstraint = null;
                        String attrEntryId = atts.getValue("attrEntryId");
                        if (CommonUtils.isNotEmpty(attrEntryId) && attrsInfo != null) {
                            DBSAttributeBase attr = attrsInfo.get(attrEntryId);
                            if (attr != null) {
                                curSavedConstraint = new DBDAttributeConstraint(attr, attr.getOrdinalPosition());
                            }
                        } 
                        if (curSavedConstraint == null) {
                            curSavedConstraint = new DBDAttributeConstraintBase();
                        }
                        String name = atts.getValue("name");
                        curSavedConstraint.setVisualPosition(CommonUtils.toInt(atts.getValue("pos")));
                        if (atts.getValue("order") != null) {
                            curSavedConstraint.setOrderPosition(CommonUtils.toInt(atts.getValue("order")));
                            curSavedConstraint.setOrderDescending(CommonUtils.toBoolean(atts.getValue("orderDesc")));
                        }
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
                case "option": {
                    if (curSavedConstraint != null) {
                        curOptionName = CommonUtils.toString(atts.getValue("name"));
                    }
                    break;
                }
            }
        }

        @Override
        public void saxEndElement(@NotNull SAXReader reader, @Nullable String namespaceURI, @NotNull String localName) throws XMLException {
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
                case "option": {
                    curOptionName = null;
                    break;
                }
            }
        }

        @Override
        public void saxText(@Nullable SAXReader reader, @NotNull String data) throws XMLException {
            if (isInValue) {
                curSavedConstraint.setValue(GeneralUtils.deserializeObject(data));
            } else if (curOptionName != null) {
                curSavedConstraint.setOption(curOptionName, GeneralUtils.deserializeObject(data));
                curSavedConstraint.setOption(curOptionName, GeneralUtils.deserializeObject(data));
            }
        }
    }
    
    private static class RestoredAttribute extends AbstractAttribute implements DBSObject, DBSAttributeBase, DBPQualifiedObject {
        private final RestoredAttribute parent;
        private final DBPDataKind dataKind;
        private final DBPDataSource dataSource;
        private final boolean isPseudoAttribute;
        
        public RestoredAttribute(@Nullable RestoredAttribute parent, Attributes atts) {
            super(
                atts.getValue("name"),
                atts.getValue("typeName"),
                CommonUtils.toInt(atts.getValue("typeId")),
                CommonUtils.toInt(atts.getValue("ordinalPosition")),
                CommonUtils.toLong(atts.getValue("maxLength")),
                CommonUtils.toInt(atts.getValue("scale"), -1),
                CommonUtils.toInt(atts.getValue("precision"), -1),
                CommonUtils.getBoolean(atts.getValue("isRequired"), false),
                CommonUtils.getBoolean(atts.getValue("isAutoGenerated"), false)
            );
            this.parent = parent;
            this.dataKind = DBPDataKind.valueOf(atts.getValue("dataKind"));
            this.isPseudoAttribute = CommonUtils.getBoolean(atts.getValue("isPseudoAttribute"), false);
            this.dataSource = null;
        }

        public RestoredAttribute(
            @Nullable RestoredAttribute boundParent,
            @NotNull RestoredAttribute original,
            @NotNull DBPDataSource dataSource
        ) {
            super(original.getName(), original.getTypeName(), original.getTypeID(),
                original.getOrdinalPosition(), original.getMaxLength(), original.getScale(),
                original.getPrecision(), original.isRequired(), original.isAutoGenerated());
            this.parent = boundParent;
            this.dataKind = original.getDataKind();
            this.isPseudoAttribute = original.isPseudoAttribute();
            this.dataSource = dataSource;
        }

        @Override
        public DBPDataKind getDataKind() {
            return dataKind;
        }
        
        public boolean isPseudoAttribute() {
            return isPseudoAttribute;
        }

        @Override
        public RestoredAttribute getParentObject() {
            return parent;
        }
        
        @Override
        public DBPDataSource getDataSource() {
            return dataSource;
        }

        @NotNull
        @Override
        public String getFullyQualifiedName(@Nullable DBPEvaluationContext context) {
            final DBPDataSource dataSource = getDataSource();
            if (getParentObject() == null) {
                return DBUtils.getQuotedIdentifier(dataSource, getName());
            }
            char structSeparator = dataSource.getSQLDialect().getStructSeparator();

            StringBuilder query = new StringBuilder();
            boolean hasPrevIdentifier = false;
            for (RestoredAttribute attribute = this; attribute != null; attribute = attribute.getParentObject()) {
                if (attribute.isPseudoAttribute() || (attribute.getParentObject() == null && attribute.getDataKind() == DBPDataKind.DOCUMENT)) {
                    // Skip pseudo attributes and document attributes (e.g. Mongo root document)
                    continue;
                }
                if (hasPrevIdentifier) {
                    query.insert(0, structSeparator);
                }
                query.insert(0, DBUtils.getQuotedIdentifier(dataSource, attribute.getName()));
                hasPrevIdentifier = true;
            }

            return query.toString();
        }
    }
}