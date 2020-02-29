/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.ui.preferences.WizardPrefPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * ActiveWizard.
 *
 */
public abstract class ActiveWizard extends BaseWizard
{
    private List<WizardPrefPage> prefPages = new ArrayList<>();

    protected WizardPrefPage addPreferencePage(IPreferencePage prefPage, String title, String description)
    {
        WizardPrefPage wizardPage = createPreferencePage(prefPage, title, description);
        addPage(wizardPage);
        return wizardPage;
    }

    protected WizardPrefPage createPreferencePage(IPreferencePage  prefPage, String title, String description) {
        WizardPrefPage wizardPage = new WizardPrefPage(prefPage, title, description);
        prefPages.add(wizardPage);
        if (prefPage instanceof IWorkbenchPropertyPage) {
            ((IWorkbenchPropertyPage) prefPage).setElement(getActiveElement());
        }
        return wizardPage;
    }

    protected IAdaptable getActiveElement() {
        return null;
    }


    @Override
    public boolean performCancel()
    {
        // Just in case - cancel changes in pref pages (there shouldn't be any)
        for (WizardPrefPage prefPage : prefPages) {
            prefPage.performCancel();
        }
        return true;
    }

    protected void savePrefPageSettings()
    {
        savePrefPageSettings(prefPages.toArray(new WizardPrefPage[0]));
    }

    private void savePrefPageSettings(WizardPrefPage[] pages)
    {
        for (WizardPrefPage prefPage : pages) {
            savePageSettings(prefPage);

            WizardPrefPage[] subPages = prefPage.getSubPages(false, true);
            if (subPages != null) {
                savePrefPageSettings(subPages);
            }
        }
    }

    private void savePageSettings(WizardPrefPage prefPage) {
        if (isPageActive(prefPage)) {
            prefPage.performFinish();
        }
    }

    protected void createPreferencePages(IPreferenceNode[] preferenceNodes) {
        createPreferencePages(null, preferenceNodes);
    }

    private void createPreferencePages(WizardPrefPage parent, IPreferenceNode[] preferenceNodes) {
        Arrays.sort(preferenceNodes, Comparator.comparing(IPreferenceNode::getLabelText));
        for (IPreferenceNode node : preferenceNodes) {
            if (isNodeHasParent(node, preferenceNodes)) {
                continue;
            }
            node.createPage();
            IPreferencePage preferencePage = node.getPage();
            if (preferencePage == null) {
                continue;
            }
            preferencePage.setContainer((IPreferencePageContainer) getContainer());
            WizardPrefPage wizardPrefPage;
            if (parent == null) {
                wizardPrefPage = addPreferencePage(preferencePage, preferencePage.getTitle(), preferencePage.getDescription());
            } else {
                wizardPrefPage = parent.addSubPage(preferencePage, preferencePage.getTitle(), preferencePage.getDescription());
            }

            IPreferenceNode[] subNodes = node.getSubNodes();
            if (subNodes != null) {
                createPreferencePages(wizardPrefPage, subNodes);
            }
        }
    }

    private boolean isNodeHasParent(IPreferenceNode node, IPreferenceNode[] allNodes) {
        for (IPreferenceNode n : allNodes) {
            for (IPreferenceNode subNode : n.getSubNodes()) {
                if (node.getId().equals(subNode.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

}