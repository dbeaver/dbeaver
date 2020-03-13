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

package org.jkiss.dbeaver.runtime.serialize;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * SerializerDescriptor
 */
public class SerializerDescriptor extends AbstractDescriptor
{
    private static final Log log = Log.getLog(SerializerDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.serialize"; //$NON-NLS-1$

    private String id;
    private ObjectType serializerType;

    SerializerDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.serializerType = new ObjectType(config.getAttribute("class"));
    }

    public String getId()
    {
        return id;
    }

    public DBPObjectSerializer createSerializer() throws DBException {
        return serializerType.createInstance(DBPObjectSerializer.class);
    }

}
