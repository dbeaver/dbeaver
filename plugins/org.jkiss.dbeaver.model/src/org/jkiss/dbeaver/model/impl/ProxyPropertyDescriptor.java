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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;

/**
 * ProxyPropertyDescriptor
*/
public class ProxyPropertyDescriptor implements DBPPropertyDescriptor
{

    protected final DBPPropertyDescriptor original;

    public ProxyPropertyDescriptor(DBPPropertyDescriptor original)
    {
        this.original = original;
    }

    @NotNull
    @Override
    public Object getId()
    {
        return this.original.getId();
    }

    @Override
    public String getCategory()
    {
        return this.original.getCategory();
    }

    @Override
    public String getDescription()
    {
        return this.original.getDescription();
    }

    @Override
    public Class<?> getDataType() {
        return original.getDataType();
    }

    @Override
    public boolean isRequired() {
        return original.isRequired();
    }

    @Override
    public boolean isRemote() {
        return original.isRemote();
    }

    @Override
    public Object getDefaultValue() {
        return original.getDefaultValue();
    }

    @Override
    public boolean isEditable(Object object) {
        return original.isEditable(object);
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        return this.original.getDisplayName();
    }


}
