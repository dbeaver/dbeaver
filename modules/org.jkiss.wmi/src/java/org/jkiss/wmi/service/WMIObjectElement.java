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

package org.jkiss.wmi.service;

import java.util.List;

/**
 * Object element (property or method)
 */
public abstract class WMIObjectElement extends WMIQualifiedObject {

    private WMIObject owner;
    private String name;

    protected WMIObjectElement(WMIObject owner, String name)
    {
        this.owner = owner;
        this.name = name;
    }

    public WMIObject getOwner()
    {
        return owner;
    }

    public String getName()
    {
        return name;
    }

    @Override
    protected void readObjectQualifiers(List<WMIQualifier> qualifiers) throws WMIException
    {
        getOwner().readQualifiers(this instanceof WMIObjectAttribute, getName(), qualifiers);
    }

}
