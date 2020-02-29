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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;

public class PostgreServerType extends AbstractDescriptor {

    private static final Log log = Log.getLog(PostgreServerTypeRegistry.class);

    private final ObjectType type;
    private final String id;
    private final String name;
    private final DBPImage icon;

    private final String defaultDatabase;
    private final String defaultUser;
    private final boolean supportsCustomConnectionURL;

    protected PostgreServerType(IConfigurationElement config) {
        super(config);
        type = new ObjectType(config.getAttribute("class"));
        id = config.getAttribute("id");
        name = config.getAttribute("name");
        icon = iconToImage(config.getAttribute("logo"));

        defaultDatabase = config.getAttribute("defaultDatabase");
        defaultUser = config.getAttribute("defaultUser");
        supportsCustomConnectionURL = CommonUtils.getBoolean(config.getAttribute("customURL"), false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public PostgreServerExtension createServerExtension(PostgreDataSource dataSource) throws DBException {
        try {
            return (PostgreServerExtension) type.getObjectClass().getConstructor(PostgreDataSource.class).newInstance(dataSource);
        } catch (Throwable e) {
            throw new DBException("Error instantiating PG server type", e);
        }
    }

    public boolean supportsCustomConnectionURL() {
        return supportsCustomConnectionURL;
    }
}
