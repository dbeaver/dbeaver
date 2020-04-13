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
package org.jkiss.dbeaver.ui.controls.resultset.panel.references;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.controls.resultset.QueryResultsDecorator;

/**
 * Decorator for grouping panel
 */
public class ReferencesResultsDecorator extends QueryResultsDecorator {

    private ReferencesResultsContainer container;

    public ReferencesResultsDecorator(ReferencesResultsContainer container) {
        this.container = container;
    }

    @Override
    public long getDecoratorFeatures() {
        return FEATURE_PRESENTATIONS;
    }

    @Override
    public void fillContributions(@NotNull IContributionManager contributionManager) {
//        contributionManager.add(new ReferencesPanel.EditColumnsAction(container));
//        contributionManager.add(new ReferencesPanel.DeleteColumnAction(container));
//        contributionManager.add(new ReferencesPanel.ClearGroupingAction(container));
    }

    @Override
    public Boolean getAutoRecordMode() {
        ReferencesResultsContainer.ReferenceKey referenceKey = container.getActiveReferenceKey();
        return referenceKey != null && !referenceKey.isReference();
    }

}
