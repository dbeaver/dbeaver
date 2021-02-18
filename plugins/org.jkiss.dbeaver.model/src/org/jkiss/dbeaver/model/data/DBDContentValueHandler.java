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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.io.IOException;
import java.io.Writer;

/**
 * Content value handler
 */
public interface DBDContentValueHandler extends DBDValueHandler
{

    /**
     * Writes content value.
     * Must use native content representation.
     */
    void writeStreamValue(DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull DBSTypedObject type, @NotNull DBDContent object, @NotNull Writer writer)
        throws DBCException, IOException;

}
