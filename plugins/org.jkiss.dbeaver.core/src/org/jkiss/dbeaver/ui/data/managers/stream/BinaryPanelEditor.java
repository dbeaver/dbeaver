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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener.PreferenceChangeEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.binary.BinaryContent;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.binary.pref.HexPreferencesPage;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
* ControlPanelEditor
*/
public class BinaryPanelEditor implements IStreamValueEditor<HexEditControl> {

    private static final Log log = Log.getLog(BinaryPanelEditor.class);

    @Override
    public HexEditControl createControl(IValueController valueController){
    	
    	HexEditControl hControl = new HexEditControl(valueController.getEditPlaceholder(), SWT.BORDER);
		DBPPreferenceListener preferencesChangeListener = new DBPPreferenceListener() {
			@Override
			public void preferenceChange(PreferenceChangeEvent event) {

				if (HexPreferencesPage.PROP_DEF_WIDTH.equals(event.getProperty())) {
					String defValue = (String) event.getNewValue();
					hControl.setDefWidth(Integer.valueOf(defValue));
				}
			}
		};
		DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
		store.addPropertyChangeListener(preferencesChangeListener);
        return hControl;
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull HexEditControl control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Prime content value", 1);
        try {
            DBDContentStorage data = value.getContents(monitor);
            String charset = null;
            monitor.subTask("Read binary value");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (data != null) {
                try (InputStream contentStream = data.getContentStream()){
                    ContentUtils.copyStreams(contentStream, -1, buffer, monitor);
                }
                charset = data.getCharset();
            } else {
                charset = DBValueFormatting.getDefaultBinaryFileEncoding(value.getDataSource());
            }
            control.setContent(buffer.toByteArray(), charset);
        } catch (IOException e) {
            throw new DBException("Error reading stream value", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull HexEditControl control, @NotNull DBDContent value) throws DBException
    {
        BinaryContent binaryContent = control.getContent();
        ByteBuffer buffer = ByteBuffer.allocate((int) binaryContent.length());
        try {
            binaryContent.get(buffer, 0);
        } catch (IOException e) {
            log.error(e);
        }
        value.updateContents(
            monitor,
            new BytesContentStorage(buffer.array(), GeneralUtils.getDefaultFileEncoding()));
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final HexEditControl control) throws DBCException {
        manager.add(new Action("Switch Insert/Overwrite mode", DBeaverIcons.getImageDescriptor(UIIcon.CURSOR)) {
            @Override
            public void run() {
                control.redrawCaret(true);
            }
        });
    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull HexEditControl control) throws DBCException {

    }

}
