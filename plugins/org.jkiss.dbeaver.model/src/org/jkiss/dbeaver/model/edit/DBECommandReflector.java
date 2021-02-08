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

import org.jkiss.dbeaver.model.DBPObject;

/**
 * Database Object Command reflector.
 * Updates UI on command undo/redo actions.
 * Note: reflector isn't invoked after command creation (because command is created AFTER UI changes).
 */
public interface DBECommandReflector<OBJECT_TYPE extends DBPObject, COMMAND extends DBECommand<OBJECT_TYPE>> {

    void redoCommand(COMMAND command);

    void undoCommand(COMMAND command);

}
