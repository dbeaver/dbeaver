/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.app.DBPProject;

import java.time.LocalDateTime;

/**
 * Access session.
 */
public interface SMSession extends DBPObject, AutoCloseable {

    /**
     * Session space
     */
    @NotNull
    SMAuthSpace getSessionSpace();

    @NotNull
    SMSessionContext getSessionContext();

    SMSessionPrincipal getSessionPrincipal();

    /**
     * Session unique ID
     */
    @NotNull
    String getSessionId();

    @NotNull
    LocalDateTime getSessionStart();

    /**
     * Application session is a global singleton session
     */
    boolean isApplicationSession();

    /**
     * Singleton session project
     */
    @Nullable
    DBPProject getSingletonProject();

    /**
     * Closes session.
     * It mustn't throw any errors.
     */
    void close();
}
