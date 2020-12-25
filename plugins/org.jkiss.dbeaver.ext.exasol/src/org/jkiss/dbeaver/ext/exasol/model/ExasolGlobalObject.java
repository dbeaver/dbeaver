/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolGlobalObject implements DBSObject, DBPSaveableObject {

    private final ExasolDataSource dataSource;
    private boolean persisted;

    protected ExasolGlobalObject(ExasolDataSource dataSource, boolean persisted) {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }


    @Nullable
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPersisted() {
        // TODO Auto-generated method stub
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {

        this.persisted = persisted;

    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        // TODO Auto-generated method stub
        return dataSource.getContainer();
    }

    @Override
    public ExasolDataSource getDataSource() {
        // TODO Auto-generated method stub
        return dataSource;
    }

}
