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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * Result set provider
 */
public interface IResultSetContainer {

    /**
     * Execution context which will be used by Results viewer to read data
     * @return execution context. Maybe null is container is not connected
     */
    @Nullable
    DBCExecutionContext getExecutionContext();

    /**
     * Hosted results controller
     * @return controller or null
     */
    @Nullable
    IResultSetController getResultSetController();

    /**
     * Data container (table or something).
     * @return data container or null
     */
    @Nullable
    DBSDataContainer getDataContainer();

    boolean isReadyToRun();

    // void
}
