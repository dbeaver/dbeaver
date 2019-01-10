/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.Map;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

    private static final String TAG_CONTAINER = "container"; //$NON-NLS-1$
    private static final String TAG_ENTITY = "entity"; //$NON-NLS-1$
    private static final String TAG_CONSTRAINT = "constraint"; //$NON-NLS-1$
    private static final String TAG_ATTRIBUTE = "attribute"; //$NON-NLS-1$
    private static final String ATTR_ID = "id"; //$NON-NLS-1$
    private static final String ATTR_NAME = "name"; //$NON-NLS-1$
    private static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
    private static final String ATTR_CUSTOM = "custom"; //$NON-NLS-1$
    private static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
    private static final String ATTR_VALUE = "value"; //$NON-NLS-1$
    private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
    private static final String TAG_COLORS = "colors";
    private static final String TAG_COLOR = "color";
    private static final String ATTR_OPERATOR = "operator";
    private static final String ATTR_FOREGROUND = "foreground";
    private static final String ATTR_BACKGROUND = "background";
    private static final String TAG_VALUE = "value";
    private static final String TAG_TRANSFORM = "transform";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_EXCLUDE = "exclude";

    private DBPDataSourceContainer dataSourceContainer;

    public DBVModel(DBPDataSourceContainer dataSourceContainer) {
        super(null, "model");
        this.dataSourceContainer = dataSourceContainer;
    }

    // Copy constructor
    public DBVModel(DBPDataSourceContainer dataSourceContainer, DBVModel source) {
        super(null, source);
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBSObjectContainer getRealContainer(DBRProgressMonitor monitor) throws DBException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource instanceof DBSObjectContainer) {
            return (DBSObjectContainer) dataSource;
        }
        log.warn("Datasource '" + dataSource + "' is not an object container");
        return null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSourceContainer.getDataSource();
    }

    /**
     * Search for virtual entity descriptor
     *
     * @param entity    entity
     * @param createNew create new entity if missing
     * @return entity virtual entity
     */
    public DBVEntity findEntity(DBSEntity entity, boolean createNew) {
        DBSObject[] path = DBUtils.getObjectPath(entity, false);
        if (path.length == 0) {
            log.warn("Empty entity path");
            return null;
        }
        if (path[0] != dataSourceContainer) {
            log.warn("Entity's root must be datasource container '" + dataSourceContainer.getName() + "'");
            return null;
        }
        DBVContainer container = this;
        for (int i = 1; i < path.length; i++) {
            DBSObject item = path[i];
            container = container.getContainer(item.getName(), createNew);
            if (container == null) {
                return null;
            }
        }
        return container.getEntity(entity.getName(), createNew);
    }

    public void serialize(XMLBuilder xml) throws IOException {
        serializeContainer(xml, this);
    }

    static private void serializeContainer(XMLBuilder xml, DBVContainer object) throws IOException {
        if (!object.hasValuableData()) {
            // nothing to save
            return;
        }
        xml.startElement(TAG_CONTAINER);
        xml.addAttribute(ATTR_NAME, object.getName());
        // Containers
        for (DBVContainer container : object.getContainers()) {
            serializeContainer(xml, container);
        }

        for (DBVEntity entity : object.getEntities()) {
            if (entity.hasValuableData()) {
                serializeEntity(xml, entity);
            }
        }

        xml.endElement();
    }

    private static void serializeEntity(XMLBuilder xml, DBVEntity entity) throws IOException {
        xml.startElement(TAG_ENTITY);
        xml.addAttribute(ATTR_NAME, entity.getName());
        if (!CommonUtils.isEmpty(entity.getDescriptionColumnNames())) {
            xml.addAttribute(ATTR_DESCRIPTION, entity.getDescriptionColumnNames());
        }
        if (!CommonUtils.isEmpty(entity.properties)) {
            for (Map.Entry<String, String> prop : entity.properties.entrySet()) {
                xml.startElement(TAG_PROPERTY);
                xml.addAttribute(ATTR_NAME, prop.getKey());
                xml.addAttribute(ATTR_VALUE, prop.getValue());
                xml.endElement();
            }
        }
        // Attributes
        for (DBVEntityAttribute attr : CommonUtils.safeCollection(entity.entityAttributes)) {
            try (final XMLBuilder.Element e3 = xml.startElement(TAG_ATTRIBUTE)) {
                xml.addAttribute(ATTR_NAME, attr.getName());
                final DBVTransformSettings transformSettings = attr.getTransformSettings();
                if (transformSettings != null && transformSettings.hasValuableData()) {
                    try (final XMLBuilder.Element e4 = xml.startElement(TAG_TRANSFORM)) {
                        if (!CommonUtils.isEmpty(transformSettings.getCustomTransformer())) {
                            xml.addAttribute(ATTR_CUSTOM, transformSettings.getCustomTransformer());
                        }
                        for (String id : CommonUtils.safeCollection(transformSettings.getIncludedTransformers())) {
                            try (final XMLBuilder.Element e5 = xml.startElement(TAG_INCLUDE)) {
                                xml.addAttribute(ATTR_ID, id);
                            }
                        }
                        for (String id : CommonUtils.safeCollection(transformSettings.getExcludedTransformers())) {
                            try (final XMLBuilder.Element e5 = xml.startElement(TAG_EXCLUDE)) {
                                xml.addAttribute(ATTR_ID, id);
                            }
                        }
                        final Map<String, String> transformOptions = transformSettings.getTransformOptions();
                        if (transformOptions != null) {
                            for (Map.Entry<String, String> prop : transformOptions.entrySet()) {
                                try (final XMLBuilder.Element e5 = xml.startElement(TAG_PROPERTY)) {
                                    if (prop.getValue() != null) {
                                        xml.addAttribute(ATTR_NAME, prop.getKey());
                                        xml.addAttribute(ATTR_VALUE, prop.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Constraints
        for (DBVEntityConstraint c : CommonUtils.safeCollection(entity.entityConstraints)) {
            if (c.hasAttributes()) {
                xml.startElement(TAG_CONSTRAINT);
                xml.addAttribute(ATTR_NAME, c.getName());
                xml.addAttribute(ATTR_TYPE, c.getConstraintType().getName());
                for (DBVEntityConstraintColumn cc : CommonUtils.safeCollection(c.getAttributeReferences(null))) {
                    xml.startElement(TAG_ATTRIBUTE);
                    xml.addAttribute(ATTR_NAME, cc.getAttributeName());
                    xml.endElement();
                }
                xml.endElement();
            }
        }
        // Colors
        if (!CommonUtils.isEmpty(entity.colorOverrides)) {
            xml.startElement(TAG_COLORS);
            for (DBVColorOverride color : entity.colorOverrides) {
                xml.startElement(TAG_COLOR);
                xml.addAttribute(ATTR_NAME, color.getAttributeName());
                xml.addAttribute(ATTR_OPERATOR, color.getOperator().name());
                if (color.getColorForeground() != null) {
                    xml.addAttribute(ATTR_FOREGROUND, color.getColorForeground());
                }
                if (color.getColorBackground() != null) {
                    xml.addAttribute(ATTR_BACKGROUND, color.getColorBackground());
                }
                if (!ArrayUtils.isEmpty(color.getAttributeValues())) {
                    for (Object value : color.getAttributeValues()) {
                        if (value == null) {
                            continue;
                        }
                        xml.startElement(TAG_VALUE);
                        xml.addText(GeneralUtils.serializeObject(value));
                        xml.endElement();
                    }
                }
                xml.endElement();
            }
            xml.endElement();
        }

        xml.endElement();
    }

    public SAXListener getModelParser() {
        return new ModelParser();
    }

    public void copyFrom(DBVModel model) {
        super.copyFrom(model);
    }

    class ModelParser implements SAXListener {
        private DBVContainer curContainer = null;
        private DBVEntity curEntity = null;
        private DBVEntityAttribute curAttribute = null;
        private DBVTransformSettings curTransformSettings = null;
        private DBVEntityConstraint curConstraint;
        private DBVColorOverride curColor;
        private boolean colorValue = false;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException {
            switch (localName) {
                case TAG_CONTAINER:
                    if (curContainer == null) {
                        curContainer = DBVModel.this;
                    } else {
                        DBVContainer container = new DBVContainer(
                            curContainer,
                            atts.getValue(ATTR_NAME));
                        curContainer.addContainer(container);
                        curContainer = container;
                    }
                    break;
                case TAG_ENTITY:
                    curEntity = new DBVEntity(
                        curContainer,
                        atts.getValue(ATTR_NAME),
                        atts.getValue(ATTR_DESCRIPTION));
                    curContainer.addEntity(curEntity);
                    break;
                case TAG_PROPERTY:
                    if (curTransformSettings != null) {
                        curTransformSettings.setTransformOption(
                            atts.getValue(ATTR_NAME),
                            atts.getValue(ATTR_VALUE));
                    } else if (curEntity != null) {
                        curEntity.setProperty(
                            atts.getValue(ATTR_NAME),
                            atts.getValue(ATTR_VALUE));
                    }
                    break;
                case TAG_CONSTRAINT:
                    if (curEntity != null) {
                        curConstraint = new DBVEntityConstraint(
                            curEntity,
                            DBSEntityConstraintType.VIRTUAL_KEY,
                            atts.getValue(ATTR_NAME));
                        curEntity.addConstraint(curConstraint);
                    }
                    break;
                case TAG_ATTRIBUTE:
                    if (curConstraint != null) {
                        curConstraint.addAttribute(atts.getValue(ATTR_NAME));
                    } else if (curAttribute != null) {
                        DBVEntityAttribute childAttribute = new DBVEntityAttribute(curEntity, curAttribute, atts.getValue(ATTR_NAME));
                        curAttribute.addChild(childAttribute);
                        curAttribute = childAttribute;
                    } else if (curEntity != null) {
                        curAttribute = new DBVEntityAttribute(curEntity, null, atts.getValue(ATTR_NAME));
                        curEntity.addVirtualAttribute(curAttribute);
                    }
                    break;
                case TAG_TRANSFORM:
                    curTransformSettings = new DBVTransformSettings();
                    curTransformSettings.setCustomTransformer(atts.getValue(ATTR_CUSTOM));
                    if (curAttribute != null) {
                        curAttribute.setTransformSettings(curTransformSettings);
                    } else if (curEntity != null) {
                        curEntity.setTransformSettings(curTransformSettings);
                    }
                    break;
                case TAG_INCLUDE:
                case TAG_EXCLUDE:
                    String transformerId = atts.getValue(ATTR_ID);
                    if (curTransformSettings != null && !CommonUtils.isEmpty(transformerId)) {
                        final DBDAttributeTransformerDescriptor transformer = dataSourceContainer.getPlatform().getValueHandlerRegistry().getTransformer(transformerId);
                        if (transformer == null) {
                            log.warn("Transformer '" + transformerId + "' not found");
                        } else {
                            curTransformSettings.enableTransformer(transformer, TAG_INCLUDE.equals(localName));
                        }
                    }
                    break;
                case TAG_COLOR:
                    if (curEntity != null) {
                        try {
                            curColor = new DBVColorOverride(
                                atts.getValue(ATTR_NAME),
                                DBCLogicalOperator.valueOf(atts.getValue(ATTR_OPERATOR)),
                                null,
                                atts.getValue(ATTR_FOREGROUND),
                                atts.getValue(ATTR_BACKGROUND)
                            );
                            curEntity.addColorOverride(curColor);
                        } catch (Throwable e) {
                            log.warn("Error reading color settings", e);
                        }
                    }
                    break;
                case TAG_VALUE:
                    if (curColor != null) {
                        colorValue = true;
                    }
                    break;
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) {
            if (colorValue) {
                curColor.addAttributeValue(GeneralUtils.deserializeObject(data));
            }
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
            switch (localName) {
                case TAG_CONTAINER:
                    curContainer = curContainer.getParentObject();
                    break;
                case TAG_ENTITY:
                    curEntity = null;
                    break;
                case TAG_CONSTRAINT:
                    curConstraint = null;
                    break;
                case TAG_ATTRIBUTE:
                    if (curAttribute != null) {
                        curAttribute = curAttribute.getParent();
                    }
                    break;
                case TAG_TRANSFORM:
                    curTransformSettings = null;
                case TAG_COLOR:
                    curColor = null;
                    break;
                case TAG_VALUE:
                    if (curColor != null) {
                        colorValue = false;
                    }
                    break;
            }

        }
    }

}
