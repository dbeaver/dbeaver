/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.DBSTablePath;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.*;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private static class TableLookupCache {
        List<DBNNode> nodes;
        boolean loading = true;
    }

    private SQLEditor editor;
    private Map<String, TableLookupCache> linksCache = new HashMap<String, TableLookupCache>();

    public SQLHyperlinkDetector(SQLEditor editor)
    {
        this.editor = editor;
    }

    public synchronized void clearCache()
    {
        linksCache.clear();
    }

    public synchronized IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || !editor.getDataSourceContainer().isConnected()) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        int offset = region.getOffset();

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(editor.getSyntaxManager().getCatalogSeparator());
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
                if (wordStart < 0 && !wordDetector.isPlainWordPart(ch)) {
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

        if (word.length() == 0) {
            return null;
        }

        if (editor.getSyntaxManager().getKeywordType(identifier.toUpperCase()) == SQLSyntaxManager.KeywordType.KEYWORD) {
            // Skip keywords
            return null;
        }

        String tableName = word.toUpperCase();
        TableLookupCache tlc = linksCache.get(tableName);
        if (tlc == null) {
            // Start new word finder job
            tlc = new TableLookupCache();
            linksCache.put(tableName, tlc);
            TablesFinderJob job = new TablesFinderJob(tableName, tlc);
            job.schedule();
        }
        if (tlc.loading) {
            // Wait for 250ms maximum
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // interrupted - just go further
                    break;
                }
                if (!tlc.loading) {
                    break;
                }
            }
        }
        if (tlc.loading) {
            // Long task - just return no links for now
            return null;
        } else {
            // If no nodes found for this word - just null result
            if (tlc.nodes.isEmpty()) {
                return null;
            }
            // Check nodes (they may be disposed by refresh/delete/other node actions)
            for (Iterator<DBNNode> i = tlc.nodes.iterator(); i.hasNext(); ) {
                if (i.next().isDisposed()) {
                    i.remove();
                }
            }
            if (tlc.nodes.isEmpty()) {
                // No more nodes remains - try next time
                linksCache.remove(tableName);
                return null;
            }
            // Create hyperlinks based on nodes
            final IRegion wordRegion = new Region(wordStart, wordEnd - wordStart);
            IHyperlink[] links = new IHyperlink[tlc.nodes.size()];
            for (int i = 0, objectsSize = tlc.nodes.size(); i < objectsSize; i++) {
                links[i] = new EntityHyperlink(tlc.nodes.get(i), wordRegion);
            }
            return links;
        }
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    private class TablesFinderJob extends DataSourceJob {

        private String word;
        private TableLookupCache cache;

        protected TablesFinderJob(String word, TableLookupCache cache)
        {
            super("Find table names for '" + word + "'", DBIcon.SQL_EXECUTE.getImageDescriptor(), editor.getDataSource());
            this.word = word;
            this.cache = cache;
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            cache.nodes = new ArrayList<DBNNode>();
            final List<DBSTablePath> pathList;
            try {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                pathList = ((DBSStructureAssistant) getDataSource()).findTableNames(monitor, word, 10);
                if (!pathList.isEmpty()) {
                    for (DBSTablePath path : pathList) {
                        DBSObject object = DBUtils.getTableByPath(monitor, (DBSEntityContainer) getDataSource(), path);
                        if (object != null) {
                            DBNNode node = navigatorModel.getNodeByObject(monitor, object, true);
                            cache.nodes.add(node);
                        }
                    }
                }
            } catch (DBException e) {
                log.warn(e);
            }
            finally {
                cache.loading = false;
            }
            return Status.OK_STATUS;
        }
    }

}