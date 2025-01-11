/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.util.List;

public class FilterResetAllTransformersAction extends AbstractResultSetViewerAction {
    public FilterResetAllTransformersAction(ResultSetViewer resultSetViewer) {
        super(resultSetViewer, ResultSetMessages.controls_resultset_viewer_action_reset_all_transformers);
    }

    @Override
    public void run() {
        execute(true);
    }

    void execute(boolean refresh) {
        final DBVEntity virtualEntity = DBVUtils.getVirtualEntity(getResultSetViewer().getDataContainer(), false);
        if (virtualEntity == null) {
            return;
        }
        if (virtualEntity.getTransformSettings() != null && virtualEntity.getTransformSettings().hasValuableData()) {
            virtualEntity.setTransformSettings(null);
        }
        List<DBVEntityAttribute> vAttrs = virtualEntity.getEntityAttributes();
        if (vAttrs != null) {
            for (DBVEntityAttribute vAttr : vAttrs) {
                vAttr.setTransformSettings(null);
            }
        }
        if (refresh) {
            getResultSetViewer().refreshData(null);
        }
    }
}
