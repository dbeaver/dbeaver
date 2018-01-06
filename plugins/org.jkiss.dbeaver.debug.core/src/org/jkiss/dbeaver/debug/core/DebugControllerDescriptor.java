/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.debug.core;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGControllerRegistry;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.utils.CommonUtils;

/**
 * ValueManagerDescriptor
 */
public class DebugControllerDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(DebugControllerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.debug.core.controllers"; //$NON-NLS-1$
    public static final String TAG_CONTROLLER = "controller"; //$NON-NLS-1$
    public static final String ATTR_DATASOURCE = "datasource"; //$NON-NLS-1$

    private String dataSourceID;
    private ObjectType implType;
    private DBGControllerRegistry isntance;

    public DebugControllerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.dataSourceID = config.getAttribute(ATTR_DATASOURCE);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    public String getDataSourceID()
    {
        return dataSourceID;
    }

    @NotNull
    public DBGControllerRegistry getInstance()
    {
        if (isntance == null ) {
            try {
                isntance = implType.createInstance(DBGControllerRegistry.class);
            } catch (Exception e) {
                throw new IllegalStateException("Can't instantiate debug controller '" + this.dataSourceID + "'", e); //$NON-NLS-1$
            }
        }
        return isntance;
    }

    @Override
    public String toString() {
        return dataSourceID;
    }
}