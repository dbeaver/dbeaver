/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.internal.ui;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.jkiss.dbeaver.debug.ui.LaunchContributionFactory;
import org.jkiss.dbeaver.debug.ui.LaunchContributionItem;
import org.jkiss.dbeaver.debug.ui.DebugUi;

public class DebugContributionFactory extends LaunchContributionFactory {

    public DebugContributionFactory() {
        super(DebugUi.DEBUG_AS_MENU_ID);
        setText(DebugUiMessages.DebugContributionFactory_text);
        setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG));
    }

    @Override
    protected LaunchContributionItem createContributionItem()
    {
        return new DebugContributionItem();
    }
}
