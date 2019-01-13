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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/**
 * AbstractContent
 *
 * @author Serge Rider
 */
public abstract class AbstractContent implements DBDContent {

    protected final DBPDataSource dataSource;
    protected boolean modified = false;

    protected AbstractContent(DBPDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public AbstractContent(AbstractContent copyFrom) {
        this.dataSource = copyFrom.dataSource;
        this.modified = copyFrom.modified;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void resetContents()
    {
        // do nothing
    }

    @Override
    public String toString()
    {
        String displayString = getDisplayString(DBDDisplayFormat.UI);
        return displayString == null ? DBConstants.NULL_VALUE_LABEL : displayString;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

}
