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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Active object (schema selector)
 */
public interface DBSObjectSelector
{

    boolean supportsDefaultChange();

    /**
     * Get active selected (default) object.
     * Returns null if there is no default object.
     */
    @Nullable
    DBSObject getDefaultObject();

    /**
     * Changes default object.
     * You may call this method only if {@link #supportsDefaultChange()} returns true.
     * Note: default object will be changed for all execution contexts of the datasource.
     */
    void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException;

    /**
     * Detects default object from the specified session.
     * If it changes from the active default object then changes it and returns true.
     * Otherwise returns false.
     */
    boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException;

}