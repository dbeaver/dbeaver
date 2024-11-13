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

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * Object which can block execution flow.
 * Such as socket, statement or connection, etc.
 */
public interface DBRBlockingObject {

    /**
     * Cancels block.
     * In actual implementation this object may not block process at the moment of invocation
     * of this method. Implementor should check object's state and cancel blocking on demand.
     * @throws DBException on error
     * @param monitor monitor
     * @param blockThread  thread which initiated the block. Can be null.
     */
    void cancelBlock(@NotNull DBRProgressMonitor monitor, @Nullable Thread blockThread) throws DBException;

    default Thread getBlockThread() {
        return null;
    }

}
