/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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