/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Abstract part listener
 */
public abstract class AbstractPageListener implements IPageListener
{
    // IPageListener

    @Override
    public void pageActivated(IWorkbenchPage page) {

    }

    @Override
    public void pageClosed(IWorkbenchPage page) {

    }

    @Override
    public void pageOpened(IWorkbenchPage page) {

    }
}
