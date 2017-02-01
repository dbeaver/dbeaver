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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSIndexType
 */
public class DBSIndexType implements DBPNamedObject
{
    public static final DBSIndexType UNKNOWN = new DBSIndexType("UNKNOWN", ModelMessages.model_struct_Unknown); //$NON-NLS-1$
    //public static final DBSIndexType STATISTIC = new DBSIndexType("STATISTIC", CoreMessages.model_struct_Statistic); //$NON-NLS-1$
    public static final DBSIndexType CLUSTERED = new DBSIndexType("CLUSTERED", ModelMessages.model_struct_Clustered); //$NON-NLS-1$
    public static final DBSIndexType HASHED = new DBSIndexType("HASHED", ModelMessages.model_struct_Hashed); //$NON-NLS-1$
    public static final DBSIndexType OTHER = new DBSIndexType("OTHER", ModelMessages.model_struct_Other); //$NON-NLS-1$

    private final String id;
    private final String name;

    public DBSIndexType(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    @NotNull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof DBSIndexType && name.equals(((DBSIndexType)obj).name);
    }
}
