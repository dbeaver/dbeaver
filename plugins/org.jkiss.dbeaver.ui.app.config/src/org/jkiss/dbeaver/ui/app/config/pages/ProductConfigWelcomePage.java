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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguageManager;
import org.jkiss.dbeaver.registry.language.PlatformLanguageRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.function.Consumer;

public class ProductConfigWelcomePage extends ProductConfigWizardPage {
    private final UIObservable<DBPPlatformLanguage> language;
    private boolean seenLanguageChangeWarning = false;

    public ProductConfigWelcomePage() {
        super(ProductConfigMessages.welcome_title, ProductConfigMessages.welcome_description);

        language = UIObservable.of(
            DBPPlatformDesktop.getInstance().getPlatformLanguage(),
            DBPPlatformLanguage.class
        );
        language.addChangeListener((o, newLanguage) -> {
            if (!seenLanguageChangeWarning) {
                seenLanguageChangeWarning = true;
                UIUtils.showMessageBox(
                    getShell(),
                    "Language change",
                    "Language change will be applied after restart.",
                    SWT.ICON_INFORMATION
                );
            }
        });
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public void applySettings() {
        if (DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager manager) {
            manager.setPlatformLanguage(language.get());
        }
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb.margins(10, 10)
            .row(rb -> rb.panel(createLanguageChooser()))
            .row(rb -> rb
                .label(lb -> lb
                    .text(ProductConfigMessages.welcome_body_text)
                    .wrap()
                    .hint(TEXT_WIDTH_HINT, TEXT_HEIGHT_HINT)
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)))
            .row(UIRowBuilder::verticalSpacer)
            .row(rb -> rb
                .panel(pb1 -> pb1
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)
                    .row(rb1 -> rb1
                        // TODO introduce a dedicated icon+label control
                        .label(lb -> lb
                            .image(DBIcon.SMALL_INFO)
                            .align(UIAlignY.TOP))
                        .label(lb -> lb
                            .text(ProductConfigMessages.welcome_body_hint)
                            .wrap()
                            .hint(TEXT_WIDTH_HINT, TEXT_HEIGHT_HINT)
                            .align(UIAlignX.FILL)
                            .grow(UIGrowX.ALWAYS)))));
    }

    @NotNull
    private Consumer<UIPanelBuilder> createLanguageChooser() {
        if (!(DBWorkbench.getPlatform() instanceof DBPPlatformLanguageManager)) {
            return UIRowBuilder.identityConsumer();
        }
        return pb -> pb.row(rb -> rb
            .label("Language:")
            .comboBox(
                PlatformLanguageRegistry.getInstance().getLanguages(),
                language,
                DBPPlatformLanguage::getLabel))
            .row(rb -> rb.label(""));
    }
}
