/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.List;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

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

    @Override
    public void persist(XMLBuilder xml) throws IOException {
        super.persist(xml);
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
            if (localName.equals(RegistryConstants.TAG_CONTAINER)) {
                if (curContainer == null) {
                    curContainer  = DBVModel.this;
                } else {
                    DBVContainer container = new DBVContainer(
                        curContainer,
                        atts.getValue(RegistryConstants.ATTR_NAME));
                    curContainer.addContainer(container);
                    curContainer = container;
                }
            } else if (localName.equals(RegistryConstants.TAG_ENTITY)) {
                curEntity = new DBVEntity(
                    curContainer,
                    atts.getValue(RegistryConstants.ATTR_NAME),
                    atts.getValue(RegistryConstants.ATTR_DESCRIPTION));
                curContainer.addEntity(curEntity);
            } else if (localName.equals(RegistryConstants.TAG_PROPERTY)) {
                if (curEntity != null) {
                    curEntity.setProperty(
                        atts.getValue(RegistryConstants.ATTR_NAME),
                        atts.getValue(RegistryConstants.ATTR_VALUE));
                }
            } else if (localName.equals(RegistryConstants.TAG_CONSTRAINT)) {
                if (curEntity != null) {
                    curConstraint = new DBVEntityConstraint(
                        curEntity,
                        DBSEntityConstraintType.VIRTUAL_KEY,
                        atts.getValue(RegistryConstants.ATTR_NAME));
                    curEntity.addConstraint(curConstraint);
                }
            } else if (localName.equals(RegistryConstants.TAG_ATTRIBUTE)) {
                if (curConstraint != null) {
                    curConstraint.addAttribute(atts.getValue(RegistryConstants.ATTR_NAME));
                }
            }
        }

        @Override
        public void saxText(SAXReader reader, String data) {}

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
            if (localName.equals(RegistryConstants.TAG_CONTAINER)) {
                curContainer = curContainer.getParentObject();
            } else if (localName.equals(RegistryConstants.TAG_ENTITY)) {
                curEntity = null;
            } else if (localName.equals(RegistryConstants.TAG_CONSTRAINT)) {
                curConstraint = null;
            }

        }
    }

}
