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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;

/**
 * Progress monitor default implementation
 */
public class ProxyProgressMonitor implements DBRProgressMonitor, IProgressMonitor {

    private DBRProgressMonitor original;

    public ProxyProgressMonitor(DBRProgressMonitor original)
    {
        this.original = original;
    }

    @Override
    public IProgressMonitor getNestedMonitor()
    {
        return original.getNestedMonitor();
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        original.beginTask(name, totalWork);
    }

    @Override
    public void done()
    {
        original.done();
    }

    @Override
    public void subTask(String name)
    {
        original.subTask(name);
    }

    @Override
    public void worked(int work)
    {
        original.worked(work);
    }

    @Override
    public boolean isCanceled()
    {
        return original.isCanceled();
    }

    @Override
    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        original.startBlock(object, taskName);
    }

    @Override
    public synchronized void endBlock()
    {
        original.endBlock();
    }

    @Override
    public List<DBRBlockingObject> getActiveBlocks()
    {
        return original.getActiveBlocks();
    }

    //////////////////////////////////////////
    // IProgressMonitor

    @Override
    public void internalWorked(double work) {

    }

    @Override
    public void setCanceled(boolean value) {

    }

    @Override
    public void setTaskName(String name) {

    }
}
