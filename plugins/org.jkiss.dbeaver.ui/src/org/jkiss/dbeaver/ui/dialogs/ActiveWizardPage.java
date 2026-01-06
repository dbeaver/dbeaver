/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.dbeaver.ui.ICompositeDialogPageContainer;

/**
 * ActiveWizardPage
 */
public abstract class ActiveWizardPage<WIZARD extends IWizard> extends WizardPage implements IWizardPageActive {

    protected ActiveWizardPage(String pageName) {
        super(pageName);
    }

    @Override
    public WIZARD getWizard() {
        return (WIZARD) super.getWizard();
    }

    /**
     * Determine if the page is complete and update the page appropriately.
     */
    protected void updatePageCompletion() {
        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
            setErrorMessage(null);
        }
    }

    @Override
    protected boolean isCurrentPage() {
        final IWizardContainer container = getContainer();
        if (container == null) {
            return false;
        }
        final IWizardPage page = container.getCurrentPage();
        return page == this
            || page instanceof ICompositeDialogPageContainer cc && cc.getCurrentSubPage() == this;
    }

    protected boolean determinePageCompletion() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            activatePage();
        }
    }

    @Override
    public void activatePage() {
    }

    @Override
    public void deactivatePage() {

    }

    public boolean isAutoResizeEnabled() {
        return true;
    }
}
