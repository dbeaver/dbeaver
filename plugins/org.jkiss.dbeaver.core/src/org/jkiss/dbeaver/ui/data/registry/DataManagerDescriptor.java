/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorDescriptor
 */
public class DataManagerDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(DataManagerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataManager"; //$NON-NLS-1$
    public static final String TAG_MANAGER = "manager"; //$NON-NLS-1$
    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$
    private static final String ATTR_KIND = "kind";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_TYPE_NAME = "typeName";
    private static final String ATTR_DATA_SOURCE = "dataSource";
    private static final String ATTR_EXTENSION = "extension";

    private String id;
    private ObjectType implType;
    private final List<SupportInfo> supportInfos = new ArrayList<>();

    private static class SupportInfo {
        String typeName;
        DBPDataKind dataKind;
        ObjectType valueType;
        String extension;
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
            String typeName = typeElement.getAttribute(ATTR_TYPE_NAME);
            String className = typeElement.getAttribute(ATTR_TYPE);
            String ext = typeElement.getAttribute(ATTR_EXTENSION);
            String dspId = typeElement.getAttribute(ATTR_DATA_SOURCE);
            if (!CommonUtils.isEmpty(kindName) || !CommonUtils.isEmpty(typeName) || !CommonUtils.isEmpty(className) || !CommonUtils.isEmpty(kindName) || !CommonUtils.isEmpty(ext)) {
                SupportInfo info = new SupportInfo();
                if (!CommonUtils.isEmpty(kindName)) {
                    try {
                        info.dataKind = DBPDataKind.valueOf(kindName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Bad data kind: " + kindName);
                    }
                }
                if (!CommonUtils.isEmpty(typeName)) {
                    info.typeName = typeName;
                }
                if (!CommonUtils.isEmpty(className)) {
                    info.valueType = new ObjectType(className);
                }
                if (!CommonUtils.isEmpty(ext)) {
                    info.extension = ext;
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

    public boolean supportsType(@Nullable DBPDataSourceContainer dataSource, DBSTypedObject typedObject, Class<?> valueType, boolean checkDataSource, boolean checkType)
    {
        final DBPDataKind dataKind = typedObject.getDataKind();
        for (SupportInfo info : supportInfos) {
            if (dataSource != null && info.dataSource != null) {
                DriverDescriptor driver = (DriverDescriptor) dataSource.getDriver();
                if (driver.getProviderDescriptor() != info.dataSource) {
                    continue;
                }
            } else if (checkDataSource) {
                continue;
            }
            if (info.typeName != null) {
                if (info.typeName.equalsIgnoreCase(typedObject.getTypeName())) {
                    return true;
                }
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
            if (info.extension != null) {
                DBSDataType dataType;
                if (typedObject instanceof DBSDataType) {
                    dataType = (DBSDataType) typedObject;
                } else if (typedObject instanceof DBSTypedObjectEx) {
                    dataType = ((DBSTypedObjectEx) typedObject).getDataType();
                } else {
                    dataType = null;
                }
                if (dataType != null && CommonUtils.equalObjects(info.extension, CommonUtils.toString(dataType.geTypeExtension()))) {
                    return true;
                }
            }
        }
        return false;
    }

}