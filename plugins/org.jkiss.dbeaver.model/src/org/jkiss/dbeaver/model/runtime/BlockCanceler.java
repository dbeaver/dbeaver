/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;

/**
 * Abstract Database Job
 */
public class BlockCanceler
{
    public static void cancelBlock(DBRProgressMonitor monitor, Thread blockActiveThread) throws DBException {
        DBRBlockingObject block = monitor.getActiveBlock();
        if (block != null) {
            final Thread thread = Thread.currentThread();
            final String threadOldName = thread.getName();
            thread.setName("Operation cancel [" + block + "]");
            try {
                block.cancelBlock(monitor, blockActiveThread);
            } catch (Throwable e) {
                throw new DBException("Cancel error", e);
            } finally {
                thread.setName(threadOldName);
            }
        }
    }

}
