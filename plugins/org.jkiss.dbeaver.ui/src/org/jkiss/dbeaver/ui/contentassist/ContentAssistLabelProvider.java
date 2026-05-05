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
package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.utils.CommonUtils;

public class ContentAssistLabelProvider extends BaseLabelProvider implements ILabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof DBPImageProvider) {
            DBPImage image = ((DBPImageProvider) element).getObjectImage();
            return image == null ? null : DBeaverIcons.getImage(image);
        }
        return null;
    }

    @Override
    public String getText(Object element) {
        return element instanceof IContentProposal ? ((IContentProposal) element).getLabel() : CommonUtils.toString(element);
    }
}
