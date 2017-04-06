/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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

package org.jkiss.dbeaver.model.admin.locks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.Collection;
import java.util.Map;

/**
 * Lock manager
 */
public interface DBAServerLockManager<LOCK_TYPE extends DBAServerLock<?>,LOCK_TYPE_ITEM extends DBAServerLockItem> {

    DBPDataSource getDataSource();
    
    Map<?,LOCK_TYPE> getLocks(DBCSession session, Map<String, Object> options)
    		throws DBException;
    
    Collection<LOCK_TYPE_ITEM> getLockItems(DBCSession session,Map<String, Object> options)
    		throws DBException;

    void alterSession(DBCSession session, LOCK_TYPE sessionType, Map<String, Object> options)
        throws DBException;

    Class<LOCK_TYPE> getLocksType();
}


