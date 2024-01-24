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

package org.jkiss.dbeaver.model.impl.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMAuthSpace;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.auth.SMSessionProviderService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Session context implementation
 */
public class SessionContextImpl implements SMSessionContext {
    private static final Log log = Log.getLog(SessionContextImpl.class);

    private final SMSessionContext parentContext;
    private final List<SMSession> sessions = new ArrayList<>();

    public SessionContextImpl(SMSessionContext parentContext) {
        this.parentContext = parentContext;
    }

    @Nullable
    @Override
    public SMSession getSpaceSession(@NotNull DBRProgressMonitor monitor, @NotNull SMAuthSpace space, boolean open) throws DBException {
        SMSession session = findSpaceSession(space);
        if (session != null) {
            return session;
        }

        //log.debug(">> Session not found in context " + this + " for space " + space);
        session = parentContext == null ? null : parentContext.getSpaceSession(monitor, space, false);
        if (session == null && open) {
            SMSessionProviderService sessionProviderService = DBWorkbench.getService(SMSessionProviderService.class);
            if (sessionProviderService != null) {
                try {
                    // Session will be added in this context by itself (if needed)
                    session = sessionProviderService.acquireSession(monitor, this, space);
                    if (session != null) {
                        addSession(session);
                    }
                } catch (Exception e) {
                    throw new DBException("Error acquiring session", e);
                }
            }
        }
        return session;
    }

    @Nullable
    @Override
    public SMAuthSpace getPrimaryAuthSpace() {
        if (CommonUtils.isEmpty(sessions)) {
            return null;
        }
        return sessions.get(0).getSessionSpace();
    }

    @Nullable
    @Override
    public SMSession findSpaceSession(@NotNull SMAuthSpace space) {
        for (SMSession session : sessions) {
            if (CommonUtils.equalObjects(session.getSessionSpace(), space)) {
                return session;
            }
        }
        return null;
    }

    public void addSession(@NotNull SMSession session) {
        if (!sessions.contains(session)) {
            sessions.add(session);
            //log.debug(">> Session added to context " + this + ", space=" + session.getSessionSpace() + ": " + session, new Exception());
        } else {
            log.debug("Session '" + session + "' was added twice");
        }
    }

    @Override
    public boolean removeSession(@NotNull SMSession session) {
        if (sessions.remove(session)) {
            //log.debug(">> Session removed from context " + this + ", space=" + session.getSessionSpace()  + ": " + session, new Exception());
            return true;
        } else {
            log.debug("Session '" + session + "' was removed twice");
            return false;
        }
    }

    public void clear() {
        this.sessions.clear();
    }

}
