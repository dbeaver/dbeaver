/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

public abstract class SQLGeneratorTable extends SQLGeneratorBase<DBSEntity> {

    protected Collection<? extends DBSEntityAttribute> getAllAttributes(DBRProgressMonitor monitor, DBSEntity object) throws DBException
    {
        return CommonUtils.safeCollection(object.getAttributes(monitor));
    }

    protected Collection<? extends DBSEntityAttribute> getKeyAttributes(DBRProgressMonitor monitor, DBSEntity object) throws DBException
    {
        return DBUtils.getBestTableIdentifier(monitor, object);
    }
}
