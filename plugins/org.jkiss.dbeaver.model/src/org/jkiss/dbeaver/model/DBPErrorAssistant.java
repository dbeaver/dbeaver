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

package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * Error assistant
 */
public interface DBPErrorAssistant
{
    enum ErrorType {
        NORMAL,
        CONNECTION_LOST,
        DRIVER_CLASS_MISSING,
        PERMISSION_DENIED
    }

    class ErrorPosition
    {
        // Line number (starts from zero)
        public int line = -1;
        // Position in line. If line < 0 then position from start of query (starts from zero)
        public int position = -1;
        // Position information
        public String info = null;

        @Override
        public String toString() {
            return line + ":" + position + (info == null ? "" : " (" + info + ")");
        }
    }

    ErrorType discoverErrorType(@NotNull DBException error);

    @Nullable
    ErrorPosition[] getErrorPosition(@NotNull Throwable error);

}