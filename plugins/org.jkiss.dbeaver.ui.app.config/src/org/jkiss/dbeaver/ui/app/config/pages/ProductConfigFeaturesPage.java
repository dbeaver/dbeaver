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
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.dbeaver.ui.app.config.ProductConfigFeaturesPanel;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;

public class ProductConfigFeaturesPage extends ProductConfigWizardPage {
    private ProductConfigFeaturesPanel features;

    public ProductConfigFeaturesPage() {
        super(ProductConfigMessages.features_title, ProductConfigMessages.features_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        if (getWizard().isFeatureUpdate()) {
            setTitle(ProductConfigMessages.features_update_title);
            setDescription(ProductConfigMessages.features_update_description);
        }
        setControl(features.createControl(parent, ProductConfigMessages.features_hint));
    }

    @Override
    public boolean isPageApplicable() {
        return !getWizard().isFeatureUpdate() || !ProductConfigRegistry.getInstance().getAvailableFeatures().isEmpty();
    }

    @Override
    public boolean isApplicableToFeatureUpdate() {
        return true;
    }

    @Override
    public void loadSettings() {
        features = new ProductConfigFeaturesPanel(getWizard().isFeatureUpdate(), getWizard().getOrigin());
    }

    @Override
    public void applySettings() {
        if (features.applySettings()) {
            getWizard().markForRestart();
        }
    }
}
