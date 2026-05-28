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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingResultsContainer;

public abstract class GroupingAction extends Action {
    protected final GroupingResultsContainer groupingResultsContainer;

    public GroupingAction(
        @NotNull GroupingResultsContainer groupingResultsContainer,
        @Nullable String text,
        @NotNull ImageDescriptor image
    ) {
        super(text, image);
        this.groupingResultsContainer = groupingResultsContainer;
    }

    public GroupingAction(
        @NotNull GroupingResultsContainer groupingResultsContainer,
        @Nullable String text,
        int style
    ) {
        super(text, style);
        this.groupingResultsContainer = groupingResultsContainer;
    }
}
