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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Content value (LOB).
 *
 * @author Serge Rider
 */
public interface DBDContent extends DBDValue {

    DBPDataSource getDataSource();

    /**
     * Content length in bytes.
     * @return length
     * @throws DBCException
     */
    long getContentLength() throws DBCException;

    /**
     * Content type (MIME).
     * @return content type
     */
    @NotNull
    String getContentType();

    String getDisplayString(DBDDisplayFormat format);

    @Nullable
    DBDContentStorage getContents(DBRProgressMonitor monitor) throws DBCException;

    /**
     * Update contents
     * @param monitor monitor
     * @param storage storage
     * @return true if implementation acquires passed storage object.
     *   false if implementation copies storage.
     * @throws DBException
     */
    boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException;

    /**
     * Resets contents changes back to original
     */
    void resetContents();

}
