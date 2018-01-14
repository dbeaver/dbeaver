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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.IFilter;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

/**
 * Default database navigator filter
 */
public class DatabaseNavigatorTreeFilter implements IFilter {
    @Override
    public boolean select(Object element) {
        if (!(element instanceof DBNDatabaseItem)) {
            return true;
        }
        DBSObject object = ((DBNDatabaseItem) element).getObject();
        return
            !(object instanceof DBSEntity) &&
            !(object instanceof DBSProcedure) &&
            !(object instanceof DBSSequence) &&
            !(object instanceof DBAUser);
    }
}
