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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Data type descriptor capable of binding its attributes to the data container context.
 */
public interface DBSBindableDataType extends DBSDataType {

    /**
     * Binds data type attributes the given data container context 
     * and returns a context-bound attributes containing corresponding information

     * @param monitor a progress monitor
     * @param memberContext concrete attribute of the container record's hierarchy containing the instance of a data type
     *     (it is usually a field of a table, or of another nested data type in the table, whose field is of this type)
     * @return a context-bound attributes containing information about the given data container context
     * @throws DBException on any DB error
     */
    @NotNull
    List<? extends DBSContextBoundAttribute> bindAttributesToContext(
        @NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding memberContext
    ) throws DBException;
}

