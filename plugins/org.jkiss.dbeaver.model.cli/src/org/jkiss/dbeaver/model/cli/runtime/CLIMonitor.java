/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;

public class CLIMonitor extends DefaultProgressMonitor {
    private static final Log log = Log.getLog(CLIMonitor.class);

    public CLIMonitor() {
        super(new CLIMonitorInternal());
    }

    private static class CLIMonitorInternal implements IProgressMonitor {
        private volatile boolean isCanceled;

        @Override
        public void beginTask(@NotNull String name, int totalWork) {
            log.debug(name);
        }

        @Override
        public void done() {
        }

        @Override
        public void internalWorked(double work) {
        }

        @Override
        public boolean isCanceled() {
            return isCanceled;
        }

        @Override
        public void setCanceled(boolean value) {
            isCanceled = value;
        }

        @Override
        public void setTaskName(@NotNull String name) {
            log.debug(name);
        }

        @Override
        public void subTask(@NotNull String name) {
            log.debug("\t" + name);
        }

        @Override
        public void worked(int work) {
        }
    }
}
