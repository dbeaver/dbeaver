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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

/**
 * Meta column mapping
 */
public class GenericMetaColumn {

    private final String id;
    private final String columnName;
    private final Integer columnIndex;
    private final boolean supported;

    public GenericMetaColumn(IConfigurationElement cfg)
    {
        this.id = cfg.getAttribute(RegistryConstants.ATTR_ID);
        this.columnName = cfg.getAttribute(RegistryConstants.ATTR_NAME);
        String indexStr = cfg.getAttribute("index");
        if (!CommonUtils.isEmpty(indexStr)) {
            this.columnIndex = Integer.valueOf(indexStr);
        } else {
            this.columnIndex = null;
        }
        String supportedStr = cfg.getAttribute("supported");
        this.supported = !"false".equals(supportedStr);
    }

    public String getId()
    {
        return id;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public int getColumnIndex()
    {
        return columnIndex;
    }

    public boolean isSupported()
    {
        return supported;
    }

    public Object getColumnIdentifier()
    {
        return columnIndex == null ? columnName : columnIndex;
    }
}
