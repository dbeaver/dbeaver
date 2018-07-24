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

package org.jkiss.dbeaver.model.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * Object change command
 */
public interface DBECommand<OBJECT_TYPE extends DBPObject> {

    String getTitle();

    OBJECT_TYPE getObject();

    boolean isUndoable();

    /**
     * Validates command.
     * If command is fine then just returns, otherwise throws an exception
     * @throws DBException contains information about invalid command state
     */
    void validateCommand() throws DBException;

    void updateModel();

    DBECommand<?> merge(
        DBECommand<?> prevCommand,
        Map<Object, Object> userParams);

    DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException;

}
