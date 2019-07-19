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

import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * Virtual database model
 */
public class DBVModel extends DBVContainer {

    private DBPDataSourceContainer dataSourceContainer;
    @NotNull
    private String id;

    public DBVModel(@NotNull String id, @NotNull Map<String, Object> map) {
        super(null, id, map);
        this.id = id;
    }

    public DBVModel(@NotNull DBPDataSourceContainer dataSourceContainer) {
        super(null, "model");
        this.dataSourceContainer = dataSourceContainer;
        this.id = dataSourceContainer.getId();
    }

    // Copy constructor
    public DBVModel(@NotNull DBPDataSourceContainer dataSourceContainer, @NotNull DBVModel source) {
        super(null, source);
        this.dataSourceContainer = dataSourceContainer;
        this.id = dataSourceContainer.getId();
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public void setDataSourceContainer(DBPDataSourceContainer dataSourceContainer) {
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

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return dataSourceContainer == null ? null : dataSourceContainer.getDataSource();
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

    public void serialize(DBRProgressMonitor monitor, JsonWriter json) throws IOException, DBException {
        DBVModelSerializerModern.serializeContainer(monitor, json, this);
    }

    @Deprecated
    public void serialize(XMLBuilder xml) throws IOException {
        DBVModelSerializerLegacy.serializeContainer(xml, this);
    }

    @Deprecated
    public SAXListener getModelParser() {
        return new DBVModelSerializerLegacy.ModelParser(this);
    }

    public void copyFrom(DBVModel model) {
        super.copyFrom(model);
    }

}
