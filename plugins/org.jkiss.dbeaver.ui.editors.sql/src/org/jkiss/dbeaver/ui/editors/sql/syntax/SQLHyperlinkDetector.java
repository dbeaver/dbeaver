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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.util.List;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector {
    static protected final Log log = Log.getLog(SQLHyperlinkDetector.class);

    private SQLContextInformer contextInformer;

    public SQLHyperlinkDetector(SQLEditorBase editor, SQLContextInformer contextInformer) {
        this.contextInformer = contextInformer;
    }

    @Nullable
    @Override
    public synchronized IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        this.contextInformer.searchInformation(region);
        if (!this.contextInformer.hasObjects()) {
            // Long task - just return no links for now
            return null;
        } else {
            // Create hyperlinks based on references
            final SQLIdentifierDetector.WordRegion wordRegion = this.contextInformer.getWordRegion();
            final IRegion hlRegion = new Region(wordRegion.identStart, wordRegion.identEnd - wordRegion.identStart);
            final List<DBSObjectReference> references = this.contextInformer.getObjectReferences();
            IHyperlink[] links = new IHyperlink[references.size()];
            for (int i = 0, objectsSize = references.size(); i < objectsSize; i++) {
                links[i] = new EntityHyperlink(contextInformer.getEditor().getSite(), references.get(i), hlRegion);
            }
            return links;
        }
    }

    public String getLastKeyword() {
        final SQLIdentifierDetector.WordRegion wordRegion = this.contextInformer.getWordRegion();
        if (wordRegion != null) {
            final IRegion hlRegion = new Region(wordRegion.identStart, wordRegion.identEnd - wordRegion.identStart);
            try {
                return contextInformer.getEditor().getDocument().get(hlRegion.getOffset(), hlRegion.getLength());
            } catch (BadLocationException e) {
                log.error(e);
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

}