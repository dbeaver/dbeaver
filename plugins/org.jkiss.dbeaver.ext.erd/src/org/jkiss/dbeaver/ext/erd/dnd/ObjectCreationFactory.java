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
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.requests.CreationFactory;

/**
 * Simple object creation factory
 */
public class ObjectCreationFactory implements CreationFactory {

    private final Object newObject;
    private final Object objectType;

    public ObjectCreationFactory(Object newObject, Object objectType)
    {
        this.newObject = newObject;
        this.objectType = objectType;
    }

    @Override
    public Object getNewObject()
    {
        return newObject;
    }

    @Override
    public Object getObjectType()
    {
        return objectType;
    }
}
