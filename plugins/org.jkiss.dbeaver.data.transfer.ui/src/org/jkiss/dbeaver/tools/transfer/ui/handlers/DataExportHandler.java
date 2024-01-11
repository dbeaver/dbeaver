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
package org.jkiss.dbeaver.tools.transfer.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.registry.BasePolicyDataProvider;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class DataExportHandler extends DataTransferHandler {

    @Override
    protected IDataTransferNode<?> adaptTransferNode(Object object) {
        final DBSDataContainer adapted = RuntimeUtils.getObjectAdapter(object, DBSDataContainer.class);
        if (adapted != null) {
            return new DatabaseTransferProducer(adapted);
        } else {
            return null;
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (BasePolicyDataProvider.getInstance().isPolicyEnabled(DTConstants.POLICY_DATA_EXPORT)) {
            UIUtils.showMessageBox(
                HandlerUtil.getActiveShell(event),
                DTUIMessages.dialog_policy_data_export_title,
                DTUIMessages.dialog_policy_data_export_msg,
                SWT.ICON_WARNING);
            return null;
        }
        return super.execute(event);
    }

}