/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;

import java.util.*;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private IDataSourceProvider dataSourceProvider;
    private SQLSyntaxManager syntaxManager;

    private static class TableLookupCache {
        List<DBNDatabaseNode> nodes;
        boolean loading = true;
    }

    private Map<String, TableLookupCache> linksCache = new HashMap<String, TableLookupCache>();


    public SQLHyperlinkDetector(IDataSourceProvider dataSourceProvider, SQLSyntaxManager syntaxManager)
    {
        this.dataSourceProvider = dataSourceProvider;
        this.syntaxManager = syntaxManager;
    }

    public synchronized IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || dataSourceProvider.getDataSource() == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        int offset = region.getOffset();

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(syntaxManager.getCatalogSeparator());
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

        if (syntaxManager.getKeywordType(identifier.toUpperCase()) == SQLSyntaxManager.KeywordType.KEYWORD) {
            // Skip keywords
            return null;
        }
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSourceProvider.getDataSource());
        if (structureAssistant == null) {
            return null;
        }

        String tableName = word.toUpperCase();
        TableLookupCache tlc = linksCache.get(tableName);
        if (tlc == null) {
            // Start new word finder job
            tlc = new TableLookupCache();
            linksCache.put(tableName, tlc);
            TablesFinderJob job = new TablesFinderJob(structureAssistant, tableName, tlc);
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
            for (Iterator<DBNDatabaseNode> i = tlc.nodes.iterator(); i.hasNext(); ) {
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
        linksCache.clear();
    }

    private class TablesFinderJob extends DataSourceJob {

        private DBSStructureAssistant structureAssistant;
        private String word;
        private TableLookupCache cache;

        protected TablesFinderJob(DBSStructureAssistant structureAssistant, String word, TableLookupCache cache)
        {
            super("Find table names for '" + word + "'", DBIcon.SQL_EXECUTE.getImageDescriptor(), dataSourceProvider.getDataSource());
            this.structureAssistant = structureAssistant;
            this.word = word;
            this.cache = cache;
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            cache.nodes = new ArrayList<DBNDatabaseNode>();
            try {
                DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
                List<DBSObjectType> objectTypes = new ArrayList<DBSObjectType>();//Arrays.asList(structureAssistant.getSupportedObjectTypes());
                objectTypes.add(RelationalObjectType.TYPE_TABLE);
                Collection<DBSObject> objects = structureAssistant.findObjectsByMask(monitor, null, objectTypes, word, 10);
                if (!objects.isEmpty()) {
                    for (DBSObject object : objects) {
                        DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, object, true);
                        if (node != null) {
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