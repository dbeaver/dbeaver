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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
* ProxyWorkbenchPart
*/
public class ProxyWorkbenchPart implements IWorkbenchPart {

    private final IWorkbenchPart part;

    public ProxyWorkbenchPart(IWorkbenchPart part) {
        this.part = part;
    }

    @Override
    public void addPropertyListener(IPropertyListener listener) {
        part.addPropertyListener(listener);
    }

    @Override
    public void createPartControl(Composite parent) {
        part.createPartControl(parent);
    }

    @Override
    public void dispose() {
        part.dispose();
    }

    @Override
    public IWorkbenchPartSite getSite() {
        return part.getSite();
    }

    @Override
    public String getTitle() {
        return part.getTitle();
    }

    @Override
    public Image getTitleImage() {
        return part.getTitleImage();
    }

    @Override
    public String getTitleToolTip() {
        return part.getTitleToolTip();
    }

    @Override
    public void removePropertyListener(IPropertyListener listener) {
        part.removePropertyListener(listener);
    }

    @Override
    public void setFocus() {
        part.setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return part.getAdapter(adapter);
    }
}
