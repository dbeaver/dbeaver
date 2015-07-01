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
package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.*;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorDescriptor
 */
public class DataManagerDescriptor extends AbstractDescriptor
{
    static final Log log = Log.getLog(DataManagerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataManager"; //$NON-NLS-1$
    public static final String TAG_MANAGER = "manager"; //$NON-NLS-1$
    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$
    private static final String ATTR_KIND = "kind";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_DATA_SOURCE = "dataSource";

    private String id;
    private ObjectType implType;
    private final List<SupportInfo> supportInfos = new ArrayList<SupportInfo>();

    private static class SupportInfo {
        DBPDataKind dataKind;
        ObjectType valueType;
        DataSourceProviderDescriptor dataSource;
    }

    private IValueManager instance;

    public DataManagerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));

        IConfigurationElement[] typeElements = config.getChildren(TAG_SUPPORTS);
        for (IConfigurationElement typeElement : typeElements) {
            String kindName = typeElement.getAttribute(ATTR_KIND);
            String typeName = typeElement.getAttribute(ATTR_TYPE);
            String dspId = typeElement.getAttribute(ATTR_DATA_SOURCE);
            if (!CommonUtils.isEmpty(kindName) || !CommonUtils.isEmpty(typeName) || !CommonUtils.isEmpty(kindName)) {
                SupportInfo info = new SupportInfo();
                if (!CommonUtils.isEmpty(kindName)) {
                    try {
                        info.dataKind = DBPDataKind.valueOf(kindName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad data kind: " + kindName);
                    }
                }
                if (!CommonUtils.isEmpty(typeName)) {
                    info.valueType = new ObjectType(typeName);
                }
                if (!CommonUtils.isEmpty(dspId)) {
                    info.dataSource = DataSourceProviderRegistry.getInstance().getDataSourceProvider(dspId);
                    if (info.dataSource == null) {
                        log.warn("Data source '" + dspId + "' not found");
                    }
                }
                supportInfos.add(info);
            }
        }
    }

    public String getId()
    {
        return id;
    }

    public IValueManager getInstance()
    {
        if (instance == null && implType != null) {
            try {
                this.instance = implType.createInstance(IValueManager.class);
            }
            catch (Exception e) {
                log.error("Can't instantiate value manager '" + this.id + "'", e); //$NON-NLS-1$
            }
        }
        return instance;
    }

    public boolean supportsType(DBPDataSource dataSource, DBPDataKind dataKind, Class<?> valueType, boolean checkDataSource, boolean checkType)
    {
        for (SupportInfo info : supportInfos) {
            if (info.dataSource != null) {
                DriverDescriptor driver = (DriverDescriptor) dataSource.getContainer().getDriver();
                if (driver.getProviderDescriptor() != info.dataSource) {
                    continue;
                }
            } else if (checkDataSource) {
                continue;
            }
            if (info.valueType != null) {
                if (info.valueType.matchesType(valueType) && info.dataKind == null || info.dataKind == dataKind) {
                    return true;
                }
            } else if (checkType) {
                continue;
            }

            if (info.dataKind != null && info.dataKind == dataKind) {
                return true;
            }
        }
        return false;
    }

}