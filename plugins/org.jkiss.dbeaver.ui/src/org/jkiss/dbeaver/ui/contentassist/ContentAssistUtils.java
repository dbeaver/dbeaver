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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CComboContentAdapter;

public class ContentAssistUtils {

    private static final Log log = Log.getLog(UIUtils.class);

    public static ContentProposalAdapter installContentProposal(Control control, IControlContentAdapter contentAdapter, IContentProposalProvider provider) {
        return installContentProposal(control, contentAdapter, provider, null, true);
    }

    public static ContentProposalAdapter installContentProposal(Control control, IControlContentAdapter contentAdapter, IContentProposalProvider provider, boolean autoActivation) {
        return installContentProposal(control, contentAdapter, provider, null, autoActivation);
    }

    public static ContentProposalAdapter installContentProposal(Control control, IControlContentAdapter contentAdapter, IContentProposalProvider provider, ILabelProvider labelProvider, boolean autoActivation) {
        IKeyLookup keyLookup = KeyLookupFactory.getDefault();
        KeyStroke keyStroke = KeyStroke.getInstance(keyLookup.getCtrl(), SWT.SPACE); //$NON-NLS-1$
        final ContentProposalAdapter proposalAdapter = new ContentProposalAdapter(
            control,
            contentAdapter,
            provider,
            keyStroke,
            autoActivation ? ".abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_$([{".toCharArray() : null);
        boolean isSingleValueAdapter = contentAdapter instanceof CComboContentAdapter || contentAdapter instanceof ComboContentAdapter;
        proposalAdapter.setProposalAcceptanceStyle(isSingleValueAdapter ? ContentProposalAdapter.PROPOSAL_REPLACE : ContentProposalAdapter.PROPOSAL_INSERT);
        proposalAdapter.setPopupSize(new Point(300, 200));
        if (labelProvider == null) {
            labelProvider = new ContentAssistLabelProvider();
        }
        proposalAdapter.setLabelProvider(labelProvider);

        //proposalAdapter.setFilterStyle(ContentProposalAdapter.FILTER_CHARACTER);
        return proposalAdapter;
    }

}
