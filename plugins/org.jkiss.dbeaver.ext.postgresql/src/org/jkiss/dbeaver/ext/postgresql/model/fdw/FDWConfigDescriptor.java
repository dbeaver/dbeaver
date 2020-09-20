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

package org.jkiss.dbeaver.ext.postgresql.model.fdw;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * FDWConfigDescriptor
 */
public class FDWConfigDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(FDWConfigDescriptor.class);

    @NotNull
    private final String id;
    @NotNull
    private final String fdwId;
    @NotNull
    private final String name;
    private final String description;
    private final DBPPropertyDescriptor[] properties;
    private final String[] foreignDatabases;

    public FDWConfigDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.fdwId = config.getAttribute("fdwId");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.foreignDatabases = CommonUtils.notEmpty(config.getAttribute("databases")).split(",");

        this.properties = PropertyDescriptor.extractPropertyGroups(config);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getFdwId() {
        return fdwId;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getForeignDatabases() {
        return foreignDatabases;
    }

    public DBPPropertyDescriptor[] getProperties() {
        return properties;
    }

    public boolean matches(DBPDataSourceContainer dataSource) {
        for (String dbId : foreignDatabases) {
            if (!dbId.isEmpty() && dataSource.getDriver().getId().contains(dbId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
