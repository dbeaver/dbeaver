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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;

/**
 * ResultSet decorator.
 */
public abstract class ResultSetDecoratorBase implements IResultSetDecorator {
    @Override
    public long getDecoratorFeatures() {
        return FEATURE_NONE;
    }

    @Override
    public String getPreferredPresentation() {
        return null;
    }

    @Override
    public IResultSetLabelProvider getDataLabelProvider() {
        return null;
    }

    @Override
    public void fillContributions(@NotNull IContributionManager contributionManager) {

    }

    @Override
    public void registerDragAndDrop(@NotNull IResultSetPresentation presentation) {

    }

    @Override
    public Boolean getAutoRecordMode() {
        return null;
    }
}
