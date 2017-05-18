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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ViewValuePanel;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;

/**
* AbstractTextPanelEditor
*/
public abstract class AbstractTextPanelEditor implements IStreamValueEditor<StyledText>, IAdaptable {

    public static final String PREF_TEXT_EDITOR_WORD_WRAP = "content.text.editor.word-wrap";
    public static final String PREF_TEXT_EDITOR_AUTO_FORMAT = "content.text.editor.auto-format";

    @Override
    public abstract StyledText createControl(IValueController valueController);

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final StyledText control) throws DBCException {

    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull final StyledText editorControl) throws DBCException {
        manager.add(new Separator());
        {
            Action wwAction = new Action("Word Wrap", Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    boolean newWW = !editorControl.getWordWrap();
                    setChecked(newWW);
                    editorControl.setWordWrap(newWW);
                    ViewValuePanel.getPanelSettings().put(PREF_TEXT_EDITOR_WORD_WRAP, newWW);
                }
            };
            wwAction.setChecked(editorControl.getWordWrap());
            manager.add(wwAction);
        }

        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null) {
            final Action afAction = new Action("Auto Format", Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    boolean newAF = !ViewValuePanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT);
                    setChecked(newAF);
                    ViewValuePanel.getPanelSettings().put(PREF_TEXT_EDITOR_AUTO_FORMAT, newAF);
                    applyEditorStyle();
                }
            };
            afAction.setChecked(ViewValuePanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT));
            manager.add(afAction);
        }
    }

    protected BaseTextEditor getTextEditor() {
        return null;
    }

    protected void initEditorSettings(StyledText control) {
        boolean wwEnabled = ViewValuePanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_WORD_WRAP);
        if (wwEnabled != control.getWordWrap()) {
            control.setWordWrap(wwEnabled);
        }
    }

    protected void applyEditorStyle() {
        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null && ViewValuePanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT)) {
            textEditor.getViewer().doOperation(ISourceViewer.FORMAT);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null) {
            if (adapter.isAssignableFrom(textEditor.getClass())) {
                return adapter.cast(textEditor);
            }
            return textEditor.getAdapter(adapter);
        }
        return null;
    }
}
