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
    private final UIObservable<Boolean> sendUsageStatistics = UIObservable.of(true, Boolean.class);

    public EasyConfigDataCollectionPage() {
        super(EasyConfigMessages.data_collection_title, EasyConfigMessages.data_collection_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .row(rb -> rb
                .weblink(
                    EasyConfigMessages.data_collection_agreement_text,
                    lb -> lb
                        .align(UIAlignX.FILL, UIAlignY.FILL)
                        .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)))
            .row(rb -> rb
                .checkBox(EasyConfigMessages.data_collection_send_usage_statistics, sendUsageStatistics));
    }
}
