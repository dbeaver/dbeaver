package org.jkiss.dbeaver.model.virtual;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
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

    static final Log log = LogFactory.getLog(DBVModel.class);
    private DBSDataSourceContainer dataSourceContainer;

    public DBVModel(DBSDataSourceContainer dataSourceContainer)
    {
        super(null, "model");
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return dataSourceContainer.getDataSource();
    }

    /**
     * Search for virtual entity descriptor
     * @param entity entity
     * @return entity virtual entity
     */
    public DBVEntity findEntity(DBSEntity entity)
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
            container = container.getContainer(item.getName());
        }
        return container.getEntity(entity.getName());
    }

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
            }

        }
    }

}
