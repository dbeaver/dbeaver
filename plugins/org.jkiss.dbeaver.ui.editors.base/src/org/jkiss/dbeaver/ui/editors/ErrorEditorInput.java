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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ErrorEditorInput
 */
public class ErrorEditorInput extends DatabaseEditorInput<DBNDatabaseNode>
{
    private final IStatus error;

    public ErrorEditorInput(@NotNull IStatus error, @Nullable DBNDatabaseNode dataSourceNode) {
        super(dataSourceNode);
        this.error = error;
    }

    public IStatus getError() {
        return error;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return UIUtils.getShardImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
    }

    @Override
    public String getToolTipText() {
        return error.getMessage();
    }

}
