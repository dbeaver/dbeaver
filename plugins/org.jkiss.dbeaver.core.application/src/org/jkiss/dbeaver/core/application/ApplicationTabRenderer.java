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
package org.jkiss.dbeaver.core.application;


import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderRenderer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * TODO: Implement correct rendering of editor tabs
 * TODO: Have to read a lot about SWT CSS rendering. Maybe, some day..
 */
@Deprecated
public class ApplicationTabRenderer extends CTabRendering {


    public ApplicationTabRenderer(CTabFolder parent) {
        super(parent);
    }

    @Override
    protected void draw(int part, int state, Rectangle bounds, GC gc) {
        super.draw(part, state, bounds, gc);
    }

}

