/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.iotdb.ui.config;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBDataSource;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBRelationalUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Map;

public class IoTDBUserManager extends AbstractObjectManager<IoTDBRelationalUser>
        implements DBEObjectMaker<IoTDBRelationalUser, IoTDBDataSource> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, IoTDBRelationalUser> getObjectsCache(IoTDBRelationalUser object) {
        return null;
    }

    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    public boolean canDeleteObject(IoTDBRelationalUser object) {
        return false;
    }

    @Override
    public IoTDBRelationalUser createNewObject(DBRProgressMonitor monitor,
                                               DBECommandContext commandContext,
                                               Object container, Object copyFrom,
                                               Map<String, Object> options) throws DBException {
        return null;
    }

    @Override
    public void deleteObject(DBECommandContext commandContext,
                             IoTDBRelationalUser object,
                             Map<String, Object> options) throws DBException {
        // no-op
    }
}
