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

public class EasyConfigDataCollectionPage extends EasyConfigWizardPage {
    public EasyConfigDataCollectionPage() {
        super(EasyConfigMessages.data_collection_title, EasyConfigMessages.data_collection_description);
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
                    .text("To improve user experience and product performance, "
                        + "DBeaver can collect some anonymous statistical data about feature usage and product configuration."
                        + "\n\n"
                        + "This data doesn't include any personal or sensitive information, such as database connection "
                        + "configuration, executed queries, or database information. The data sent complies with the "
                        + "DBeaver Corporation Privacy Policy."
                        + "\n\n"
                        + "DBeaver sends statistics before shutting down or during startup. Information we collect includes:"
                        + "\n - Brief information about your OS and locale"
                        + "\n - List of UI actions you do to better understand users' workflow"
                        + "\n - Databases you use to improve support for popular ones")
                    .wrap()
                    .align(UIAlignX.FILL, UIAlignY.FILL)
                    .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)))
            .row(rb -> rb
                .checkBox("Send anonymous usage statistics", UIObservable.of(true, Boolean.class)));
    }
}
