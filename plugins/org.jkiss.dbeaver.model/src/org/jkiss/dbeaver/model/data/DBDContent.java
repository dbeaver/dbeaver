/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Content value (LOB).
 *
 * @author Serge Rider
 */
public interface DBDContent extends DBDValue {

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
