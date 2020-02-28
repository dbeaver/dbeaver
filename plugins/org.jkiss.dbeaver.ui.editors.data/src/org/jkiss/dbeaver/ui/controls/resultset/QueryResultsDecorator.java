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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

/**
 * Decorator for query results
 */
public class QueryResultsDecorator extends ResultSetDecoratorBase {

    @Override
    public long getDecoratorFeatures() {
        return FEATURE_FILTERS | FEATURE_STATUS_BAR | FEATURE_PANELS | FEATURE_EDIT;
    }

    @Override
    public String getEmptyDataMessage() {
        return ResultSetMessages.sql_editor_resultset_filter_panel_control_no_data;
    }

    @Override
    public String getEmptyDataDescription() {
        return null;
    }

}
