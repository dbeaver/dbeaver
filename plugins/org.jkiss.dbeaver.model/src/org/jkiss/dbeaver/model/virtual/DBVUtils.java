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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Virtual model utils
 */
public abstract class DBVUtils {

    @Nullable
    public static DBVTransformSettings getTransformSettings(@NotNull DBDAttributeBinding binding, boolean create) {
        DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
        if (entityAttribute != null) {
            DBVEntity vEntity = findVirtualEntity(entityAttribute.getParentObject(), create);
            if (vEntity != null) {
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, create);
                if (vAttr != null) {
                    return getTransformSettings(vAttr, create);
                }
            }
        }
        return null;
    }

    @Nullable
    public static DBVTransformSettings getTransformSettings(@NotNull DBVEntityAttribute attribute, boolean create) {
        if (attribute.getTransformSettings() != null) {
            return attribute.getTransformSettings();
        } else if (create) {
            attribute.setTransformSettings(new DBVTransformSettings());
            return attribute.getTransformSettings();
        }
        for (DBVObject object = attribute.getParentObject(); object != null; object = object.getParentObject()) {
            if (object.getTransformSettings() != null) {
                return object.getTransformSettings();
            }
        }
        return null;
    }

    @NotNull
    public static Map<String, String> getAttributeTransformersOptions(@NotNull DBDAttributeBinding binding) {
        Map<String, String> options = null;
        final DBVTransformSettings transformSettings = getTransformSettings(binding, false);
        if (transformSettings != null) {
            options = transformSettings.getTransformOptions();
        }
        if (options != null) {
            return options;
        }
        return Collections.emptyMap();
    }

    @Nullable
    public static DBVEntity findVirtualEntity(@NotNull DBSEntity source, boolean create)
    {
        return source.getDataSource().getContainer().getVirtualModel().findEntity(source, create);
    }

    @Nullable
    public static DBDAttributeTransformer[] findAttributeTransformers(@NotNull DBDAttributeBinding binding, @Nullable Boolean custom)
    {
        DBPDataSource dataSource = binding.getDataSource();
        DBPDataSourceContainer container = dataSource.getContainer();
        List<? extends DBDAttributeTransformerDescriptor> tdList =
            container.getApplication().getValueHandlerRegistry().findTransformers(dataSource, binding.getAttribute(), custom);
        if (tdList == null || tdList.isEmpty()) {
            return null;
        }
        boolean filtered = false;
        DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
        if (entityAttribute != null) {
            DBVEntity vEntity = findVirtualEntity(entityAttribute.getParentObject(), false);
            if (vEntity != null) {
                DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, false);
                if (vAttr != null) {
                    final DBVTransformSettings transformSettings = getTransformSettings(vAttr, false);
                    if (transformSettings != null) {
                        filtered = transformSettings.filterTransformers(tdList);
                    }
                }
            }
        }

        if (!filtered) {
            // Leave only default transformers
            for (int i = 0; i < tdList.size();) {
                if (tdList.get(i).isCustom() || !tdList.get(i).isApplicableByDefault()) {
                    tdList.remove(i);
                } else {
                    i++;
                }
            }
        }
        if (tdList.isEmpty()) {
            return null;
        }
        DBDAttributeTransformer[] result = new DBDAttributeTransformer[tdList.size()];
        for (int i = 0; i < tdList.size(); i++) {
            result[i] = tdList.get(i).getInstance();
        }
        return result;
    }

}
