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
import org.jkiss.code.Nullable;

/**
 * ResultSet decorator.
 */
public interface IResultSetDecorator {

    long FEATURE_NONE               = 0;
    long FEATURE_FILTERS            = 1;
    long FEATURE_STATUS_BAR         = 2;
    long FEATURE_PANELS             = 4;
    long FEATURE_EDIT               = 8;
    long FEATURE_PRESENTATIONS      = 16;
    long FEATURE_LINKS              = 32;

    long getDecoratorFeatures();

    /**
     * Primary presentation ID.
     * @return presentation ID or null.
     */
    @Nullable
    String getPreferredPresentation();

    @Nullable
    String getEmptyDataMessage();

    @Nullable
    String getEmptyDataDescription();

    IResultSetLabelProvider getDataLabelProvider();

    /**
     * Fill additional menu actions
     */
    void fillContributions(@NotNull IContributionManager contributionManager);

    void registerDragAndDrop(@NotNull IResultSetPresentation presentation);

    @Nullable
    Boolean getAutoRecordMode();
}
