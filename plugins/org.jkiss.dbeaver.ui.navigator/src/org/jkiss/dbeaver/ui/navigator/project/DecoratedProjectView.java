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
package org.jkiss.dbeaver.ui.navigator.project;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;

/**
 * DecoratedProjectView
 */
public abstract class DecoratedProjectView extends NavigatorViewBase {

    final ILabelDecorator labelDecorator;

    DecoratedProjectView() {
        labelDecorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
        labelDecorator.addListener(event -> {
                Object[] elements = event.getElements();
                if (elements != null) {
                    getNavigatorViewer().update(elements, null);
                }
            }
        );
    }


    @Override
    public void dispose() {
        super.dispose();
        labelDecorator.dispose();
    }

}
