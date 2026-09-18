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
package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CComboContentAdapter;
import org.jkiss.utils.CommonUtils;

import java.util.Objects;
import java.util.function.Supplier;

public class ContentAssistUtils {

    private static final Log log = Log.getLog(UIUtils.class);
    private static final Object SYNTHETIC_PROPOSAL_ACCEPTANCE_EVENT = new Object();

    public static final String PROPOSAL_ACTIVATION_KEY = "SQLEditor.ContentAssistant.proposal.activation.key";
    public static final String LEGACY_TAB_AUTOCOMPLETION = "SQLEditor.ContentAssistant.autocompletion.tab";

    public enum ProposalActivationKey {
        ENTER(true, false),
        TAB(false, true),
        BOTH(true, true),
        NONE(false, false);

        private final boolean acceptsEnter;
        private final boolean acceptsTab;

        ProposalActivationKey(boolean acceptsEnter, boolean acceptsTab) {
            this.acceptsEnter = acceptsEnter;
            this.acceptsTab = acceptsTab;
        }

        public boolean acceptsEnter() {
            return acceptsEnter;
        }

        public boolean acceptsTab() {
            return acceptsTab;
        }

        @NotNull
        public static ProposalActivationKey fromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            if (preferenceStore.contains(PROPOSAL_ACTIVATION_KEY)
                && !preferenceStore.isDefault(PROPOSAL_ACTIVATION_KEY)
            ) {
                return valueByName(preferenceStore.getString(PROPOSAL_ACTIVATION_KEY));
            }
            // Preserve a legacy data-source override when the new value is inherited from the global store.
            if (preferenceStore.contains(LEGACY_TAB_AUTOCOMPLETION)) {
                return preferenceStore.getBoolean(LEGACY_TAB_AUTOCOMPLETION) ? BOTH : ENTER;
            }
            if (!preferenceStore.isDefault(PROPOSAL_ACTIVATION_KEY)) {
                return valueByName(preferenceStore.getString(PROPOSAL_ACTIVATION_KEY));
            }
            String legacyTabAutocomplete = preferenceStore.getString(LEGACY_TAB_AUTOCOMPLETION);
            if (!CommonUtils.isEmpty(legacyTabAutocomplete)) {
                return Boolean.parseBoolean(legacyTabAutocomplete) ? BOTH : ENTER;
            }
            return valueByName(preferenceStore.getDefaultString(PROPOSAL_ACTIVATION_KEY));
        }

        @NotNull
        public static ProposalActivationKey defaultFromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            return valueByName(preferenceStore.getDefaultString(PROPOSAL_ACTIVATION_KEY));
        }

        @NotNull
        private static ProposalActivationKey valueByName(String name) {
            return CommonUtils.valueOf(ProposalActivationKey.class, name, BOTH);
        }
    }

    public static ContentProposalAdapter installContentProposal(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider
    ) {
        return installContentProposal(control, contentAdapter, provider, null, true);
    }

    public static ContentProposalAdapter installContentProposalWithPreferences(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider,
        @NotNull Supplier<DBPPreferenceStore> preferenceStoreSupplier
    ) {
        return installContentProposal(control, contentAdapter, provider, null, null, true, preferenceStoreSupplier);
    }

    public static ContentProposalAdapter installContentProposal(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider,
        boolean autoActivation
    ) {
        return installContentProposal(control, contentAdapter, provider, null, autoActivation);
    }

   
    public static ContentProposalAdapter installContentProposal(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider,
        @Nullable ILabelProvider labelProvider,
        boolean autoActivation
    ) {
        return installContentProposal(control, contentAdapter, provider, labelProvider, null, autoActivation);
    }
    
   
    
    public static ContentProposalAdapter installContentProposal(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider,
        @Nullable ILabelProvider labelProvider,
        @Nullable Boolean replace,
        boolean autoActivation
    ) {
        return installContentProposal(
            control,
            contentAdapter,
            provider,
            labelProvider,
            replace,
            autoActivation,
            () -> DBWorkbench.getPlatform().getPreferenceStore()
        );
    }

    private static ContentProposalAdapter installContentProposal(
        @NotNull Control control,
        @NotNull IControlContentAdapter contentAdapter,
        @NotNull IContentProposalProvider provider,
        @Nullable ILabelProvider labelProvider,
        @Nullable Boolean replace,
        boolean autoActivation,
        @NotNull Supplier<DBPPreferenceStore> preferenceStoreSupplier
    ) {
        IKeyLookup keyLookup = KeyLookupFactory.getDefault();
        KeyStroke keyStroke = KeyStroke.getInstance(keyLookup.getCtrl(), SWT.SPACE); //$NON-NLS-1$
        ContentProposalAdapter[] proposalAdapterReference = new ContentProposalAdapter[1];
        installProposalActivationKeyHandler(control, () -> proposalAdapterReference[0], preferenceStoreSupplier);
        final ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
            control,
            contentAdapter,
            provider,
            keyStroke,
            autoActivation ? ".abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$([{".toCharArray() : null);
        proposalAdapterReference[0] = proposalAdapter;
        boolean isSingleValueAdapter;
        isSingleValueAdapter = Objects.requireNonNullElseGet(
            replace,
            () -> contentAdapter instanceof CComboContentAdapter || contentAdapter instanceof ComboContentAdapter
        );
        proposalAdapter.setProposalAcceptanceStyle(isSingleValueAdapter ? ContentProposalAdapter.PROPOSAL_REPLACE : ContentProposalAdapter.PROPOSAL_INSERT);
        proposalAdapter.setPopupSize(new Point(300, 200));
        if (labelProvider == null) {
            labelProvider = new ContentAssistLabelProvider();
        }
        proposalAdapter.setLabelProvider(labelProvider);

        //proposalAdapter.setFilterStyle(ContentProposalAdapter.FILTER_CHARACTER);
        return proposalAdapter;
    }

    public static void installProposalActivationKeyHandler(
        @NotNull Control control,
        @NotNull Supplier<? extends ContentProposalAdapter> proposalAdapterSupplier
    ) {
        installProposalActivationKeyHandler(
            control,
            proposalAdapterSupplier,
            () -> DBWorkbench.getPlatform().getPreferenceStore()
        );
    }

    public static void installProposalActivationKeyHandler(
        @NotNull Control control,
        @NotNull Supplier<? extends ContentProposalAdapter> proposalAdapterSupplier,
        @NotNull Supplier<DBPPreferenceStore> preferenceStoreSupplier
    ) {
        Listener listener = event -> {
            if (event.data == SYNTHETIC_PROPOSAL_ACCEPTANCE_EVENT) {
                return;
            }
            ContentProposalAdapter proposalAdapter = proposalAdapterSupplier.get();
            if (proposalAdapter == null || !proposalAdapter.isProposalPopupOpen()) {
                return;
            }
            ProposalActivationKey activationKey = ProposalActivationKey.fromPreferences(preferenceStoreSupplier.get());
            boolean tabPressed = event.character == SWT.TAB || event.keyCode == SWT.TAB;
            boolean enterPressed = !tabPressed && (event.character == SWT.CR || event.character == SWT.LF);
            if (enterPressed && !activationKey.acceptsEnter() || tabPressed && !activationKey.acceptsTab()) {
                proposalAdapter.closeProposalPopup();
            } else if (tabPressed) {
                if (event.type == SWT.Traverse) {
                    event.detail = SWT.TRAVERSE_NONE;
                    event.doit = false;
                    if (control instanceof StyledText) {
                        // StyledText consumes Tab during traversal, so its proposal adapter never receives KeyDown.
                        Event acceptanceEvent = new Event();
                        acceptanceEvent.character = SWT.CR;
                        acceptanceEvent.keyCode = SWT.CR;
                        acceptanceEvent.stateMask = event.stateMask;
                        acceptanceEvent.data = SYNTHETIC_PROPOSAL_ACCEPTANCE_EVENT;
                        control.notifyListeners(SWT.KeyDown, acceptanceEvent);
                        return;
                    }
                }
                event.character = SWT.CR;
                event.keyCode = SWT.CR;
            }
        };
        control.addListener(SWT.KeyDown, listener);
        control.addListener(SWT.Traverse, listener);
    }

}
