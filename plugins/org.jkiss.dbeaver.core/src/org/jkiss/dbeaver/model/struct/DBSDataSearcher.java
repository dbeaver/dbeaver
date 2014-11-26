/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;

/**
 * Data searcher.
 */
public interface DBSDataSearcher extends DBSDataContainer {

    public static final long FLAG_FAST_SEARCH        = 1 >> 16;
    public static final long FLAG_SEARCH_LOBS        = 1 >> 17;
    public static final long FLAG_SEARCH_NUMBERS     = 1 >> 18;
    public static final long FLAG_CASE_SENSITIVE     = 1 >> 18;

    @NotNull
    DBCStatistics findRows(
            @NotNull DBCSession session,
            @NotNull DBDDataReceiver dataReceiver,
            @NotNull String searchString,
            long flags)
        throws DBCException;

}
