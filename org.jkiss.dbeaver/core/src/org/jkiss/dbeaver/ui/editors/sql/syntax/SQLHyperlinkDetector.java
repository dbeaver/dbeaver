/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.NullLoadService;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.ArrayList;
import java.util.List;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private SQLEditor editor;

    public SQLHyperlinkDetector(SQLEditor editor)
    {
        this.editor = editor;
    }

    // TODO: implement load service
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || !editor.getDataSourceContainer().isConnected()) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        ILoadService loadService = new NullLoadService();
        int offset = region.getOffset();

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(editor.getSyntaxManager());
        int docLength = document.getLength();
        int identStart = offset;
        int identEnd = offset;
        int wordStart = -1, wordEnd = -1;
        String identifier, word;
        try {
            if (!wordDetector.isPlainWordPart(document.getChar(offset))) {
                return null;
            }
            while (identStart >= 0) {
                char ch = document.getChar(identStart);
                if (!wordDetector.isWordPart(ch)) {
                    break;
                }
                if (!wordDetector.isPlainWordPart(ch)) {
                    wordStart = identStart + 1;
                }
                identStart--;
            }
            identStart++;
            while (identEnd < docLength) {
                char ch = document.getChar(identEnd);
                if (!wordDetector.isWordPart(ch)) {
                    break;
                }
                if (!wordDetector.isPlainWordPart(ch)) {
                    wordEnd = identEnd;
                }
                identEnd++;
            }
            if (wordStart < 0) wordStart = identStart;
            if (wordEnd < 0) wordEnd = identEnd;
            identifier = document.get(identStart, identEnd - identStart);
            word = document.get(wordStart, wordEnd - wordStart);
        } catch (BadLocationException e) {
            return null;
        }

        if (editor.getSyntaxManager().getKeywordType(identifier.toUpperCase()) != null) {
            // Skip keywords
            return null;
        }
        if (word.length() == 0) {
            return null;
        }
        // Detect what all this means
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource instanceof DBSStructureContainer && dataSource instanceof DBSStructureAssistant) {
            try {
                List<DBSTablePath> pathList = ((DBSStructureAssistant) editor.getDataSource()).findTableNames(word, 2);
                if (pathList.isEmpty()) {
                    return null;
                }
                IRegion wordRegion = new Region(wordStart, wordEnd - wordStart);
                List<IHyperlink> links = new ArrayList<IHyperlink>();
                for (DBSTablePath path : pathList) {
                    DBMNode node = null;
                    DBSObject object = DBSUtils.getTableByPath((DBSStructureContainer) dataSource, path);
                    if (object != null) {
                        node = DBeaverCore.getInstance().getMetaModel().getNodeByObject(
                            object,
                            true,
                            loadService);
                    }
                    if (node != null) {
                        links.add(new EntityHyperlink(node, wordRegion));
                    }
                }
                if (links.isEmpty()) {
                    return null;
                }
                return links.toArray(new IHyperlink[links.size()]);
            } catch (DBException e) {
                log.error(e);
                return null;
            }
        }
        return null;
    }

}