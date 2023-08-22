// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import org.eclipse.swt.graphics.Point;
import org.eclipse.jface.wizard.IWizardPage;
import java.util.Iterator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIMessages;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;

class MockDataConfigurationWizardDialog extends TaskConfigurationWizardDialog
{
    private final MockDataSettings mockDataSettings;
    
    MockDataConfigurationWizardDialog(final IWorkbenchWindow window, final TaskConfigurationWizard wizard, final MockDataSettings mockDataSettings) {
        super(window, wizard);
        this.mockDataSettings = mockDataSettings;
    }
    
    protected void finishPressed() {
        if (this.validateProperties(this.getCurrentPage())) {
            return;
        }
        int tablesToBeCleaned = 0;
        for (final EntityProperties properties : this.mockDataSettings.getEntityPropertiesList()) {
            if (properties.isRemoveOldData()) {
                ++tablesToBeCleaned;
            }
        }
        if (tablesToBeCleaned == 0) {
            super.finishPressed();
            return;
        }
        String question;
        if (tablesToBeCleaned > 1) {
            question = NLS.bind(MockDataUIMessages.tools_mockdata_wizard_page_settings_confirm_delete_old_data_from_multiple_tables_message, (Object)tablesToBeCleaned);
        }
        else if (this.mockDataSettings.getEntityPropertiesList().size() > 1) {
            question = MockDataUIMessages.tools_mockdata_wizard_page_settings_confirm_delete_old_data_from_one_table_when_generating_data_for_multiple_entities_message;
        }
        else {
            question = MockDataUIMessages.tools_mockdata_wizard_page_settings_confirm_delete_old_data_message;
        }
        if (!UIUtils.confirmAction(this.getShell(), MockDataUIMessages.tools_mockdata_wizard_title, question)) {
            return;
        }
        super.finishPressed();
    }
    
    public void nextPressed() {
        final IWizardPage currentPage = this.getCurrentPage();
        if (currentPage instanceof MockDataWizardPageSettings && this.validateProperties(currentPage)) {
            return;
        }
        super.nextPressed();
    }
    
    private boolean validateProperties(final IWizardPage currentPage) {
        if (currentPage instanceof MockDataWizardPageSettings && !((MockDataWizardPageSettings)currentPage).validateProperties()) {
            this.setErrorMessage(MockDataUIMessages.tools_mockdata_wizard_negative_numeric_error);
            return true;
        }
        return false;
    }
    
    protected Point getInitialSize() {
        return new Point(850, 550);
    }
}
