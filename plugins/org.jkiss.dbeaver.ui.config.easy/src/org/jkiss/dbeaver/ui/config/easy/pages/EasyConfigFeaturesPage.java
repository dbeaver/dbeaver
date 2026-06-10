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

public class EasyConfigFeaturesPage extends EasyConfigWizardPage {
    public EasyConfigFeaturesPage() {
        super(EasyConfigMessages.features_title, EasyConfigMessages.features_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .accept(buildFeaturesPanel());
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildFeaturesPanel() {
        return pb -> pb
            .row(rb -> rb
                .label(lb -> lb
                    .text("DBeaver comes with a lot of features that suit different workflows, "
                        + "and here you can configure them for your needs:")
                    .wrap()
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.scrolledPanel(false, true, pb1 -> pb1
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                .indent(pb2 -> pb2
                    .row(rb1 -> rb1.checkBox("AI integration", UIObservable.of(true)))
                    .row(rb1 -> rb1.checkBox("Cloud integration (AWS, Azure, Google Cloud)", UIObservable.of(true)))
                    .row(rb1 -> rb1.checkBox("Database dashboards", UIObservable.of(true)))
                    .row(rb1 -> rb1.checkBox("Git version control", UIObservable.of(true)))
                    .row(rb1 -> rb1.checkBox("Tableau integration", UIObservable.of(true)))
                    .row(rb1 -> rb1.checkBox("Procedure debugger", UIObservable.of(true))))));
    }
}
