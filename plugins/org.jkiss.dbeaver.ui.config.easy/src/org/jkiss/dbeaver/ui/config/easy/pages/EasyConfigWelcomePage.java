/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.easy.pages;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.BaseThemeSettings;
import org.jkiss.dbeaver.ui.config.easy.internal.EasyConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.function.Consumer;

public class EasyConfigWelcomePage extends EasyConfigWizardPage {

    public EasyConfigWelcomePage() {
        super(EasyConfigMessages.welcome_title, EasyConfigMessages.welcome_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .row(rb -> rb
                .label(lb -> lb
                    .text(UIObservables.bind(EasyConfigMessages.welcome_body_title, GeneralUtils.getProductTitle()))
                    .font(UIObservable.of(BaseThemeSettings.instance.partTitleBoldFont, Font.class))))
            .row(rb -> rb
                .label(lb -> lb
                    .text(EasyConfigMessages.welcome_body_text)
                    .wrap()
                    .align(UIAlignX.FILL, UIAlignY.FILL)
                    .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)));
    }
}
