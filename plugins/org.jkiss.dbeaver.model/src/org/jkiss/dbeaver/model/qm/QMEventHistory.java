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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;

import java.util.Date;
import java.util.List;

/**
 * Query manager history
 */
public interface QMEventHistory {

    long getHistorySize();

    List<QMMetaEvent> readEventHistory(
        @Nullable QMObjectType objectType,
        @Nullable Date startDate,
        @Nullable Date endDate,
        int maxEvents);

    List<QMMSessionInfo> getSessionHistory(
        @Nullable String containerId,
        @Nullable Date startDate,
        @Nullable Date endDate,
        int maxSessions);

    List<QMMStatementExecuteInfo> getQueryHistory(
        @Nullable String containerId,
        @Nullable DBCExecutionPurpose queryPurpose,
        @Nullable Date startDate,
        @Nullable Date endDate,
        int maxQueries);

}
