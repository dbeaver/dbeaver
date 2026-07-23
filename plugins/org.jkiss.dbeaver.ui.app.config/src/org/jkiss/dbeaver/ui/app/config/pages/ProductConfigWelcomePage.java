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
package org.jkiss.dbeaver.ui.app.config.pages;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.forms.UIAlignX;
import org.jkiss.dbeaver.ui.forms.UIGrowX;
import org.jkiss.dbeaver.ui.forms.UIPanelBuilder;

import java.util.function.Consumer;

public class ProductConfigWelcomePage extends ProductConfigWizardPage {
    public ProductConfigWelcomePage() {
        super(ProductConfigMessages.welcome_title, ProductConfigMessages.welcome_description);
        setPageComplete(true);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb
                .label(lb -> lb
                    .text(ProductConfigMessages.welcome_body_text)
                    .wrap()
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)));
    }
}
