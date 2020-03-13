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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
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
import java.util.Map;

/**
 * DBVModelSerializerLegacy
 */
@Deprecated
class DBVModelSerializerLegacy implements DBVModelSerializer
{
    private static final Log log = Log.getLog(DBVModelSerializerLegacy.class);

    static void serializeContainer(XMLBuilder xml, DBVContainer object) throws IOException {
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
        if (!CommonUtils.isEmpty(entity.getProperties())) {
            for (Map.Entry<String, Object> prop : entity.getProperties().entrySet()) {
                xml.startElement(TAG_PROPERTY);
                xml.addAttribute(ATTR_NAME, prop.getKey());
                xml.addAttribute(ATTR_VALUE, CommonUtils.toString(prop.getValue()));
                xml.endElement();
            }
        }
        // Attributes
        for (DBVEntityAttribute attr : CommonUtils.safeCollection(entity.getEntityAttributes())) {
            if (!attr.hasValuableData()) {
                continue;
            }
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
                        final Map<String, Object> transformOptions = transformSettings.getTransformOptions();
                        if (transformOptions != null) {
                            for (Map.Entry<String, Object> prop : transformOptions.entrySet()) {
                                try (final XMLBuilder.Element e5 = xml.startElement(TAG_PROPERTY)) {
                                    if (prop.getValue() != null) {
                                        xml.addAttribute(ATTR_NAME, prop.getKey());
                                        xml.addAttribute(ATTR_VALUE, CommonUtils.toString(prop.getValue()));
                                    }
                                }
                            }
                        }
                    }
                }
                if (!CommonUtils.isEmpty(attr.getProperties())) {
                    for (Map.Entry<String, Object> prop : attr.getProperties().entrySet()) {
                        xml.startElement(TAG_PROPERTY);
                        xml.addAttribute(ATTR_NAME, prop.getKey());
                        xml.addAttribute(ATTR_VALUE, CommonUtils.toString(prop.getValue()));
                        xml.endElement();
                    }
                }
            }
        }
        // Constraints
        for (DBVEntityConstraint c : CommonUtils.safeCollection(entity.getConstraints())) {
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
        // Foreign keys
        for (DBVEntityForeignKey fk : CommonUtils.safeCollection(entity.getForeignKeys())) {
            xml.startElement(TAG_ASSOCIATION);
            DBSEntity refEntity = fk.getAssociatedEntity();
            xml.addAttribute(ATTR_ENTITY, DBUtils.getObjectFullId(refEntity));
            DBSEntityConstraint refConstraint = fk.getReferencedConstraint();
            if (refConstraint != null) {
                xml.addAttribute(ATTR_CONSTRAINT, refConstraint.getName());
            }
            for (DBVEntityForeignKeyColumn cc : CommonUtils.safeCollection(fk.getAttributes())) {
                xml.startElement(TAG_ATTRIBUTE);
                xml.addAttribute(ATTR_NAME, cc.getAttributeName());
                xml.endElement();
            }
            xml.endElement();
        }
        // Colors
        if (!CommonUtils.isEmpty(entity.getColorOverrides())) {
            xml.startElement(TAG_COLORS);
            for (DBVColorOverride color : entity.getColorOverrides()) {
                xml.startElement(TAG_COLOR);
                xml.addAttribute(ATTR_NAME, color.getAttributeName());
                xml.addAttribute(ATTR_OPERATOR, color.getOperator().name());
                if (color.isRange()) {
                    xml.addAttribute(ATTR_RANGE, true);
                }
                if (color.isSingleColumn()) {
                    xml.addAttribute(ATTR_SINGLE_COLUMN, true);
                }
                if (color.getColorForeground() != null) {
                    xml.addAttribute(ATTR_FOREGROUND, color.getColorForeground());
                }
                if (color.getColorForeground2() != null) {
                    xml.addAttribute(ATTR_FOREGROUND2, color.getColorForeground2());
                }
                if (color.getColorBackground() != null) {
                    xml.addAttribute(ATTR_BACKGROUND, color.getColorBackground());
                }
                if (color.getColorBackground2() != null) {
                    xml.addAttribute(ATTR_BACKGROUND2, color.getColorBackground2());
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

    static class ModelParser implements SAXListener {
        private DBVContainer rootContainer;
        private DBVContainer curContainer = null;
        private DBVEntity curEntity = null;
        private DBVEntityAttribute curAttribute = null;
        private DBVTransformSettings curTransformSettings = null;
        private DBVEntityConstraint curConstraint;
        private DBVColorOverride curColor;
        private boolean colorValue = false;

        public ModelParser(DBVContainer rootContainer) {
            this.rootContainer = rootContainer;
        }

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException {
            switch (localName) {
                case TAG_CONTAINER:
                    if (curContainer == null) {
                        curContainer = rootContainer;
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
                    } else if (curAttribute != null) {
                        curAttribute.setProperty(
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
                        curEntity.addConstraint(curConstraint, false);
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
                        curEntity.addVirtualAttribute(curAttribute, false);
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
                        final DBDAttributeTransformerDescriptor transformer = DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(transformerId);
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
                            curColor.setRange(CommonUtils.getBoolean(atts.getValue(ATTR_RANGE), false));
                            curColor.setSingleColumn(CommonUtils.getBoolean(atts.getValue(ATTR_SINGLE_COLUMN), false));
                            curColor.setColorForeground2(atts.getValue(ATTR_FOREGROUND2));
                            curColor.setColorBackground2(atts.getValue(ATTR_BACKGROUND2));
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
