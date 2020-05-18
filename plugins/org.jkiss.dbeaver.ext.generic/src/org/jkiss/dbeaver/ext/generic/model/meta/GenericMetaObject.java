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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Meta object description
 */
public class GenericMetaObject {

    private final String type;
    private final String readQuery;
    private final Map<String, GenericMetaColumn> columnsMap = new HashMap<>();

    public GenericMetaObject(IConfigurationElement cfg)
    {
        this.type = cfg.getAttribute("type");
        this.readQuery = cfg.getAttribute("read-query");
        for (IConfigurationElement columnCfg : cfg.getChildren("column")) {
            GenericMetaColumn column = new GenericMetaColumn(columnCfg);
            columnsMap.put(column.getId(), column);
        }
    }

    public String getType()
    {
        return type;
    }

    public String getReadQuery()
    {
        return readQuery;
    }

    public GenericMetaColumn getColumn(String id)
    {
        return columnsMap.get(id);
    }

}
