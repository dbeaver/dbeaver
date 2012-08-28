package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

    private DBSDataSourceContainer dataSourceContainer;

    public DBVModel(DBSDataSourceContainer dataSourceContainer)
    {
        super(null, dataSourceContainer.getName());
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
        throws DBException
    {
        List<DBSObject> path = DBUtils.getObjectPath(entity, false);
        if (path.isEmpty()) {
            throw new DBException("Empty entity path");
        }
        if (path.get(0) != dataSourceContainer) {
            throw new DBException("Entity's root must be datasource container '" + dataSourceContainer.getName() + "'");
        }
        DBVContainer container = this;
        for (int i = 1; i < path.size(); i++) {
            DBSObject item = path.get(i);
            container = container.getContainer(item.getName());
        }
        return container.getEntity(entity.getName());
    }

}
