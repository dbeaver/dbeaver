/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.import_config.wizards.navicat;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class ConfigImportWizardPageNavicatSettings extends WizardPage {
	
	private TextWithOpenFile filePathText;
	private File inputFile;
	private Combo encodingCombo;
	
	protected ConfigImportWizardPageNavicatSettings()
    {
        super("Navicat");
        setTitle("Navicat");
        setDescription("Import Navicat connections");
    }

	@Override
	public void createControl(Composite parent) {
		Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));
        
        UIUtils.createControlLabel(placeholder, "Navicat Connection Export (NCX) file");
        filePathText = new TextWithOpenFile(
            placeholder,
            "Navicat Connection Export (NCX) file",
            new String[] {"*.ncx"});
        filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filePathText.getTextControl().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inputFile = new File(filePathText.getText());
                if (!inputFile.exists()) {
                    setErrorMessage("File '" + inputFile.getAbsolutePath() + "' doesn't exist");
                } else {
                    setErrorMessage(null);
                }
                getWizard().getContainer().updateButtons();
            }
        });
        
        UIUtils.createControlLabel(placeholder, "File encoding");
        encodingCombo = UIUtils.createEncodingCombo(placeholder, GeneralUtils.DEFAULT_ENCODING);
        
        setControl(placeholder);
	}
	
	@Override
    public boolean isPageComplete() {
        return inputFile != null && inputFile.exists();
    }
	
	public String getInputFileEncoding() {
        return encodingCombo.getText();
    }
	
	public File getInputFile() {
		return inputFile;
	}
}
