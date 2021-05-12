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

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.exec.DBCSession;

/**
 * DBEObjectManager
 */
public interface DBEObjectManager<OBJECT_TYPE extends DBPObject> {

    /**
     * New object container.
     * Usually it is a navigator node (DBNNode).
     */
    String OPTION_CONTAINER = "container";
    /**
     * Object type (class)
     */
    String OPTION_OBJECT_TYPE = "objectType";
    String OPTION_DELETE_CASCADE = "deleteCascade";
    String OPTION_CLOSE_EXISTING_CONNECTIONS = "closeExistingConnections";
    String OPTION_UI_SOURCE = "uiSource";
    String OPTION_ACTIVE_EDITOR = "activeEditor";

    void executePersistAction(
        DBCSession session,
        DBECommand<OBJECT_TYPE> command,
        DBEPersistAction action)
        throws DBException;

}