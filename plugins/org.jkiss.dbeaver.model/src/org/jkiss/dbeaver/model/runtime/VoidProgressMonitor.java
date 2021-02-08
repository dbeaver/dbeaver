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
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.List;

/**
 * Progress monitor null implementation
 */
public class VoidProgressMonitor implements DBRProgressMonitor {

    private final IProgressMonitor NESTED_INSTANCE = new NullProgressMonitor();

    public VoidProgressMonitor() {
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return NESTED_INSTANCE;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
    }

    @Override
    public void done()
    {
    }

    @Override
    public void subTask(String name)
    {
    }

    @Override
    public void worked(int work)
    {
    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    @Override
    public void startBlock(DBRBlockingObject object, String taskName)
    {
        // do nothing
    }

    @Override
    public void endBlock()
    {
        // do nothing
    }

    @Override
    public List<DBRBlockingObject> getActiveBlocks()
    {
        return null;
    }

}