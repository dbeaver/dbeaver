/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.data;

import org.eclipse.swt.graphics.Point;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;

/**
 * Value hint action handler
 */
public interface DBDValueHintActionHandler {

    @NotNull
    String getActionText();

    /**
     * Perform hint action
     *
     * @param controller result set  controller
     * @param location   cell location in screen coordinates
     * @param state      modifier state
     * @throws DBException on any DB error
     */
    void performAction(@NotNull IResultSetController controller, @NotNull Point location, long state) throws DBException;

}
