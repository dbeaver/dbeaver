/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;

public interface SQLEditorPresentation {

    void createPresentation(Composite parent, SQLEditor editor);

    /**
     * A notification that this presentation is about to be shown.
     *
     * @param editor associated SQL editor
     * @param isNew  {@code true} if this presentation was opened for the very first
     *               time in the associated SQL editor, or {@code false} if it was
     *               a subsequent opening.
     */
    default void showPresentation(@NotNull SQLEditor editor, boolean isNew) {
        // do nothing by default
    }

    /**
     * A notification that this presentation is about to be closed. An implementation
     * may opt not to be closed. This can be useful if an interactive confirmation is
     * shown with an option to cancel the operation.
     *
     * @param editor associated SQL editor
     * @return {@code true} if the presentation can be closed, or {@code false} if not
     */
    default boolean hidePresentation(@NotNull SQLEditor editor) {
        return true;
    }

    void dispose();

    ISelectionProvider getSelectionProvider();
}
