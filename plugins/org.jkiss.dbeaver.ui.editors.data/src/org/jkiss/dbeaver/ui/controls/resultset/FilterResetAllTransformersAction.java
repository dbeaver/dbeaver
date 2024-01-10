/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.util.List;

class FilterResetAllTransformersAction extends Action {
    private final ResultSetViewer resultSetViewer;

    FilterResetAllTransformersAction(ResultSetViewer resultSetViewer) {
        super(ResultSetMessages.controls_resultset_viewer_action_reset_all_transformers);
        this.resultSetViewer = resultSetViewer;
    }

    @Override
    public void run() {
        execute(true);
    }

    void execute(boolean refresh) {
        final DBVEntity virtualEntity = DBVUtils.getVirtualEntity(resultSetViewer.getDataContainer(), false);
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
            resultSetViewer.refreshData(null);
        }
    }
}
