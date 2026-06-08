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

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.config.easy.nls.EasyConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.function.Consumer;

public class EasyConfigSampleDatabasePage extends EasyConfigWizardPage {
    public EasyConfigSampleDatabasePage() {
        super(EasyConfigMessages.sample_database_title, EasyConfigMessages.sample_database_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb.label(lb -> lb
                .text("DBeaver comes with a handy sample database powered by SQLite that you can use "
                    + "to explore the features and capabilities of the application.")
                .wrap()
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.checkBox("Create sample database", UIObservable.of(true, Boolean.class)))
            .row(UIRowBuilder::spacer)
            .row(rb -> rb.label(lb -> lb
                .text("You can also enable tips that will appear every time you start DBeaver to help you get "
                    + "familiar with the application and learn some useful features.")
                .wrap()
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.checkBox("Show tips", UIObservable.of(true, Boolean.class)));
    }
}
