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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

/**
 * AbstractTableIndexColumn
 */
public abstract class AbstractTableIndexColumn implements DBSTableIndexColumn, DBPImageProvider
{

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Nullable
    @Override
    public DBPImage getObjectImage()
    {
        return DBValueFormatting.getObjectImage(getTableColumn());
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute()
    {
        return getTableColumn();
    }
}
