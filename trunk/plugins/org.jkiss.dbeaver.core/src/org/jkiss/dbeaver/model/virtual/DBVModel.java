/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

    private static final String TAG_CONTAINER = "container"; //$NON-NLS-1$
    private static final String TAG_ENTITY = "entity"; //$NON-NLS-1$
    private static final String TAG_CONSTRAINT = "constraint"; //$NON-NLS-1$
    private static final String TAG_ATTRIBUTE = "attribute"; //$NON-NLS-1$
    private static final String ATTR_NAME = "name"; //$NON-NLS-1$
    private static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
    private static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
    private static final String ATTR_VALUE = "value"; //$NON-NLS-1$
    private static final String ATTR_TYPE = "type"; //$NON-NLS-1$

    private DBSDataSourceContainer dataSourceContainer;

    public DBVModel(DBSDataSourceContainer dataSourceContainer)
    {
        super(null, "model");
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBSObjectContainer getRealContainer(DBRProgressMonitor monitor) throws DBException
    {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource instanceof DBSObjectContainer) {
            return (DBSObjectContainer) dataSource;
        }
        log.warn("Datasource '" + dataSource.getClass().getName() + "' is not an object container");
        return null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return dataSourceContainer.getDataSource();
    }

    /**
     * Search for virtual entity descriptor
     *
     * @param entity entity
     * @param createNew
     * @return entity virtual entity
     */
    public DBVEntity findEntity(DBSEntity entity, boolean createNew)
    {
        List<DBSObject> path = DBUtils.getObjectPath(entity, false);
        if (path.isEmpty()) {
            log.warn("Empty entity path");
            return null;
        }
        if (path.get(0) != dataSourceContainer) {
            log.warn("Entity's root must be datasource container '" + dataSourceContainer.getName() + "'");
            return null;
        }
        DBVContainer container = this;
        for (int i = 1; i < path.size(); i++) {
            DBSObject item = path.get(i);
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
        xml.endElement();
    }

    public SAXListener getModelParser()
    {
        return new ModelParser();
    }

    public void copyFrom(DBVModel model) {
        super.copyFrom(model);
    }

    class ModelParser implements SAXListener
    {
        private DBVContainer curContainer = null;
        private DBVEntity curEntity = null;
        private DBVEntityConstraint curConstraint;

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts)
            throws XMLException
        {
            if (localName.equals(TAG_CONTAINER)) {
                if (curContainer == null) {
                    curContainer  = DBVModel.this;
                } else {
                    DBVContainer container = new DBVContainer(
                        curContainer,
                        atts.getValue(ATTR_NAME));
                    curContainer.addContainer(container);
                    curContainer = container;
                }
            } else if (localName.equals(TAG_ENTITY)) {
                curEntity = new DBVEntity(
                    curContainer,
                    atts.getValue(ATTR_NAME),
                    atts.getValue(ATTR_DESCRIPTION));
                curContainer.addEntity(curEntity);
            } else if (localName.equals(TAG_PROPERTY)) {
                if (curEntity != null) {
                    curEntity.setProperty(
                        atts.getValue(ATTR_NAME),
                        atts.getValue(ATTR_VALUE));
                }
            } else if (localName.equals(TAG_CONSTRAINT)) {
                if (curEntity != null) {
                    curConstraint = new DBVEntityConstraint(
                        curEntity,
                        DBSEntityConstraintType.VIRTUAL_KEY,
                        atts.getValue(ATTR_NAME));
                    curEntity.addConstraint(curConstraint);
                }
            } else if (localName.equals(TAG_ATTRIBUTE)) {
                if (curConstraint != null) {
                    curConstraint.addAttribute(atts.getValue(ATTR_NAME));
                }
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) {}

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
            if (localName.equals(TAG_CONTAINER)) {
                curContainer = curContainer.getParentObject();
            } else if (localName.equals(TAG_ENTITY)) {
                curEntity = null;
            } else if (localName.equals(TAG_CONSTRAINT)) {
                curConstraint = null;
            }

        }
    }

}
