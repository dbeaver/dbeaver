/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class SQLEditorSourceViewer extends ProjectionViewer {

    private final LinkedList<VerifyKeyListener> verifyKeyListeners = new LinkedList<>();
    private final Supplier<DBPPreferenceStore> currentPrefStoreSupplier;
    
    /**
     * Creates an instance of this class with the given parameters.
     *
     * @param parent the SWT parent control
     * @param ruler the vertical ruler (annotation area)
     * @param overviewRuler the overview ruler
     * @param showsAnnotationOverview <code>true</code> if the overview ruler should be shown
     * @param styles the SWT style bits
     */
    public SQLEditorSourceViewer(
            @NotNull Composite parent,
            @Nullable IVerticalRuler ruler,
            @Nullable IOverviewRuler overviewRuler,
            boolean showsAnnotationOverview,
            int styles,
            @NotNull Supplier<DBPPreferenceStore> currentPrefStoreSupplier
        ) {
        super( parent, ruler, overviewRuler, showsAnnotationOverview, styles );
        this.currentPrefStoreSupplier = currentPrefStoreSupplier;
    }

    void refreshTextSelection(){
        ITextSelection selection = (ITextSelection)getSelection();
        fireSelectionChanged(selection.getOffset(), selection.getLength());
    }

    @Override
    protected StyledText createTextWidget(Composite parent, int styles) {
        StyledText textWidget = super.createTextWidget(parent, styles);
        textWidget.addListener(ST.VerifyKey, event -> {
            // It is a hack to allow auto-complete with TAB key (#2316)
            // TODO: perhaps we should test ContentAssistant.isProposalPopupActive() here?
            switch (event.type) {
                case ST.VerifyKey: {
                    if (event.character == '\t'
                        && currentPrefStoreSupplier.get().getBoolean(SQLPreferenceConstants.TAB_AUTOCOMPLETION)
                    ) {
                        VerifyEvent verifyEvent = new VerifyEvent(event);
                        verifyEvent.character = '\n';
                        for (VerifyKeyListener listener : List.copyOf(verifyKeyListeners)) {
                            listener.verifyKey(verifyEvent);
                        }
                        event.doit = verifyEvent.doit;
                    }
                    break;
                }
            }
        });
        
        //textWidget.setAlwaysShowScrollBars(false);
        return textWidget;
    }

    // Let source viewer reconfiguration possible (https://dbeaver.io/forum/viewtopic.php?f=2&t=2939)
    public void setHyperlinkPresenter(IHyperlinkPresenter hyperlinkPresenter) throws IllegalStateException {
        if (fHyperlinkManager != null) {
            fHyperlinkManager.uninstall();
            fHyperlinkManager= null;
        }
        super.setHyperlinkPresenter(hyperlinkPresenter);
    }
    
    @Override
    public void prependVerifyKeyListener(VerifyKeyListener listener) {
        verifyKeyListeners.addFirst(listener);
        super.prependVerifyKeyListener(listener);
    }
    
    @Override
    public void appendVerifyKeyListener(VerifyKeyListener listener) {
        verifyKeyListeners.addLast(listener);
        super.appendVerifyKeyListener(listener);
    }
    
    @Override
    public void removeVerifyKeyListener(VerifyKeyListener listener) {
        verifyKeyListeners.remove(listener);
        super.removeVerifyKeyListener(listener);
    }
}