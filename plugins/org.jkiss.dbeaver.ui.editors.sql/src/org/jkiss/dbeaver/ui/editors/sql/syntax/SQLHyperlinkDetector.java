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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.impl.struct.DirectObjectReference;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLDocumentSyntaxContext;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolByDbObjectDefinition;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLQuerySymbolEntry;

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
        if (!this.contextInformer.hasObjects() || this.contextInformer.getKeywordType() == DBPKeywordType.KEYWORD) {
            // Long task - just return no links for now
            return findLocalScopeReference(region.getOffset());
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
    
    private IHyperlink[] findLocalScopeReference(int offset) {
        SQLEditorBase editor = contextInformer.getEditor();
        SQLDocumentSyntaxContext context = editor.getSyntaxContext();
        if (context != null) {
            SQLQuerySymbolEntry token = context.findToken(offset);
            if (token != null) {
                final IRegion refRegion = new Region(context.getLastAccessedTokenOffset(), token.getInterval().length());
                if (token.getDefinition() instanceof SQLQuerySymbolEntry def) {
                    // TODO consider multiple definitions
                    Interval interval = def.getInterval();
                    final IRegion defRegion = new Region(interval.a + context.getLastAccessedScriptElementOffset(), interval.length());
                    return new IHyperlink[] {
                        new IHyperlink() {
                            @Override
                            public IRegion getHyperlinkRegion() {
                                return refRegion;
                            }

                            @Override
                            public String getTypeLabel() {
                                return null;
                            }

                            @Override
                            public String getHyperlinkText() {
                                return def.getName();
                            }

                            @Override
                            public void open() {
                                TextViewer textViewer = editor.getTextViewer();
                                if (textViewer != null) {
                                    textViewer.setSelectedRange(defRegion.getOffset(), defRegion.getLength());
                                    textViewer.revealRange(defRegion.getOffset(), defRegion.getLength());
                                }
                            }
                        }
                    };
                } else if (token.getDefinition() instanceof SQLQuerySymbolByDbObjectDefinition def
                    && def.getDbObject().getDataSource().getContainer() != null
                ) {
                    return new IHyperlink[] {
                        new EntityHyperlink(
                            editor.getSite(),
                            new DirectObjectReference(def.getDbObject().getParentObject(), null, def.getDbObject()),
                            refRegion
                        )
                    };
                }
            }
        }
        return null;
    }

    public String getLastKeyword() {
        final SQLIdentifierDetector.WordRegion wordRegion = this.contextInformer.getWordRegion();
        if (wordRegion != null) {
            final IRegion hlRegion = new Region(wordRegion.identStart, wordRegion.identEnd - wordRegion.identStart);
            try {
                IDocument document = contextInformer.getEditor().getDocument();
                if (document != null) {
                    return contextInformer.getEditor().getDocument().get(hlRegion.getOffset(), hlRegion.getLength());
                }
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