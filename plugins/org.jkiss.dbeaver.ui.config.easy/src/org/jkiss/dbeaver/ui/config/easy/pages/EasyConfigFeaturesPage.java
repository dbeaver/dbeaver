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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.config.easy.nls.EasyConfigMessages;
import org.jkiss.dbeaver.ui.config.easy.spi.EasyConfigFeatureDescriptor;
import org.jkiss.dbeaver.ui.config.easy.spi.EasyConfigRegistry;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EasyConfigFeaturesPage extends EasyConfigWizardPage {
    private final Map<EasyConfigFeatureDescriptor, UIObservable<Boolean>> features = new HashMap<>();

    public EasyConfigFeaturesPage() {
        super(EasyConfigMessages.features_title, EasyConfigMessages.features_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public void loadSettings() {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        for (EasyConfigFeatureDescriptor descriptor : EasyConfigRegistry.getInstance().getFeatures()) {
            var value = CommonUtils.toBoolean(store.getString(descriptor.getPreferenceKey()), descriptor.isEnabled());
            features.put(descriptor, UIObservable.of(value));
        }
    }

    @Override
    public void applySettings() {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        for (Map.Entry<EasyConfigFeatureDescriptor, UIObservable<Boolean>> entry : features.entrySet()) {
            store.setValue(entry.getKey().getPreferenceKey(), String.valueOf(entry.getValue().get()));
        }
        PrefUtils.savePreferenceStore(store);
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .accept(buildFeaturesPanel());
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildFeaturesPanel() {
        return pb -> pb
            .row(rb -> rb
                .label(lb -> lb
                    .text("DBeaver comes with a lot of features that suit different workflows, "
                        + "and here you can configure them for your needs:")
                    .wrap()
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.scrolledPanel(
                false, true, pb1 -> pb1
                    .align(UIAlignX.FILL, UIAlignY.FILL)
                    .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                    .indent(pb2 -> {
                        for (EasyConfigFeatureDescriptor descriptor : EasyConfigRegistry.getInstance().getFeatures()) {
                            pb2.row(rb1 -> rb1.checkBox(descriptor.getLabel(), features.get(descriptor)));
                        }
                    })
            ));
    }
}
