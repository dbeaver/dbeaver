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
package org.jkiss.dbeaver.ui.app.config;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.app.config.pages.ProductConfigWizardPage;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.dbeaver.ui.statistics.UIStatisticsActivator;

import java.util.function.Consumer;

public class ProductConfigDataCollectionPage extends ProductConfigWizardPage {
    private final boolean collectionRequired = DBWorkbench.getPlatform().getApplication().isStatisticsCollectionRequired();
    private final UIObservable<Boolean> sendUsageStatistics = UIObservable.of(null, Boolean.class);

    public ProductConfigDataCollectionPage() {
        super(ProductConfigMessages.data_collection_title, ProductConfigMessages.data_collection_description);
        setPageComplete(false);

        sendUsageStatistics.addChangeListener((o, n) -> setPageComplete(n != null));
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public void loadSettings() {
        if (collectionRequired) {
            sendUsageStatistics.set(true);
        } else if (UIStatisticsActivator.isSkipDataShareConfirmation()) {
            sendUsageStatistics.set(UIStatisticsActivator.isTrackingEnabled());
        }
    }

    @Override
    public void applySettings() {
        UIStatisticsActivator.setTrackingEnabled(sendUsageStatistics.get());
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .row(buildAgreementPanel())
            .accept(buildConfirmationPanel(sendUsageStatistics, collectionRequired));
    }

    @NotNull
    private static Consumer<UIRowBuilder> buildAgreementPanel() {
        return rb -> rb
            .scrolledPanel(false, true, pb1 -> pb1
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                .row(rb1 -> rb1
                    .weblink(ProductConfigMessages.data_collection_agreement_text, lb -> lb
                        .align(UIAlignX.FILL, UIAlignY.FILL)
                        .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS))));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildConfirmationPanel(
        @NotNull UIObservable<Boolean> sendUsageStatistics,
        boolean collectionRequired
    ) {
        return pb -> pb
            .row(rb -> {
                rb.radioButton(
                    ProductConfigMessages.data_collection_send_usage_statistics, bb -> bb
                        .selected(UIObservables.equals(sendUsageStatistics, Boolean.TRUE))
                        .enabled(UIObservable.of(!collectionRequired)));

                if (collectionRequired) {
                    rb.label(lb -> lb
                        .image(DBIcon.SMALL_INFO)
                        .tooltip(ProductConfigMessages.data_collection_cannot_opt_out_notice));
                }
            })
            .row(rb -> rb.radioButton(
                ProductConfigMessages.data_collection_do_not_send_usage_statistics, bb -> bb
                    .selected(UIObservables.equals(sendUsageStatistics, Boolean.FALSE))
                    .enabled(UIObservable.of(!collectionRequired))));
    }
}
