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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.DBException;

/**
 * Abstract Database Job
 */
public class BlockCanceler extends Job
{
    private static final long INTERRUPT_TIMEOUT = 2000;

    private final Thread thread;
    private boolean blockCanceled = false;

    public BlockCanceler(Thread thread) {
        super("Interrupter of " + thread.getName());
        setSystem(true);
        setUser(false);
        this.thread = thread;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (!blockCanceled) {
            thread.interrupt();
        }

        return Status.OK_STATUS;
    }

    public static void cancelBlock(DBRProgressMonitor monitor, Thread blockActiveThread) throws DBException {
        BlockCanceler canceler = null;
        if (blockActiveThread != null) {
            // Schedule thread interrupt job
            canceler = new BlockCanceler(blockActiveThread);
            canceler.schedule(INTERRUPT_TIMEOUT);
        }

        DBRBlockingObject block = monitor.getActiveBlock();
        if (block != null) {
            final Thread thread = Thread.currentThread();
            final String threadOldName = thread.getName();
            thread.setName("Operation cancel [" + block + "]");
            try {
                block.cancelBlock();
                if (canceler != null) {
                    canceler.blockCanceled = true;
                }
            } catch (Throwable e) {
                throw new DBException("Cancel error", e);
            } finally {
                thread.setName(threadOldName);
            }
        }
    }

}
