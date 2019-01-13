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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Abstract database object type.
 * Used by structure assistants
 */
public class AbstractObjectType implements DBSObjectType {

    private final String typeName;
    private final String description;
    private final DBPImage image;
    private final Class<? extends DBSObject> objectClass;

    public AbstractObjectType(String typeName, String description, DBPImage image, Class<? extends DBSObject> objectClass)
    {
        this.typeName = typeName;
        this.description = description;
        this.image = image;
        this.objectClass = objectClass;
    }

    @Override
    public String getTypeName()
    {
        return typeName;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBPImage getImage()
    {
        return image;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass()
    {
        return objectClass;
    }

}
