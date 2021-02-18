/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import java.util.List;

/**
 * Database progress monitor.
 * Similar to IProgressMonitor but with DBP specific features
 */
public interface DBRProgressMonitor {

    /**
     * Obtains eclipse progress monitor.
     * Can be used to pass to eclipse API.
     */
    IProgressMonitor getNestedMonitor();

    void beginTask(String name, int totalWork);

    void done();

    void subTask(String name);

    void worked(int work);

    boolean isCanceled();

    void startBlock(DBRBlockingObject object, String taskName);

    void endBlock();

    List<DBRBlockingObject> getActiveBlocks();

}
