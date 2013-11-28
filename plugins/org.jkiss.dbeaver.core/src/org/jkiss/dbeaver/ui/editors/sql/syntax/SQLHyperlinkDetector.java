/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.utils.CommonUtils;

import java.util.*;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private IDataSourceProvider dataSourceProvider;
    private SQLSyntaxManager syntaxManager;

    private static class ObjectLookupCache {
        List<DBSObjectReference> references;
        boolean loading = true;
    }

    private Map<String, ObjectLookupCache> linksCache = new HashMap<String, ObjectLookupCache>();


    public SQLHyperlinkDetector(IDataSourceProvider dataSourceProvider, SQLSyntaxManager syntaxManager)
    {
        this.dataSourceProvider = dataSourceProvider;
        this.syntaxManager = syntaxManager;
    }

    @Override
    public synchronized IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || dataSourceProvider.getDataSource() == null) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(syntaxManager.getStructSeparator(), syntaxManager.getQuoteSymbol());
        SQLIdentifierDetector.WordRegion wordRegion = wordDetector.detectIdentifier(document, region);

        if (wordRegion.word.length() == 0) {
            return null;
        }

        if (syntaxManager.getKeywordManager().getKeywordType(wordRegion.identifier) == DBPKeywordType.KEYWORD) {
            // Skip keywords
            return null;
        }
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, dataSourceProvider.getDataSource());
        if (structureAssistant == null) {
            return null;
        }

        String tableName = wordRegion.word;
        ObjectLookupCache tlc = linksCache.get(tableName);
        if (tlc == null) {
            // Start new word finder job
            tlc = new ObjectLookupCache();
            linksCache.put(tableName, tlc);
            TablesFinderJob job = new TablesFinderJob(structureAssistant, tableName, wordDetector.isQuoted(wordRegion.identifier), tlc);
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
            // If no references found for this word - just null result
            if (tlc.references.isEmpty()) {
                return null;
            }
            if (tlc.references.isEmpty()) {
                // No more references remains - try next time
                linksCache.remove(tableName);
                return null;
            }
            // Create hyperlinks based on references
            final IRegion hlRegion = new Region(wordRegion.wordStart, wordRegion.wordEnd - wordRegion.wordStart);
            IHyperlink[] links = new IHyperlink[tlc.references.size()];
            for (int i = 0, objectsSize = tlc.references.size(); i < objectsSize; i++) {
                links[i] = new EntityHyperlink(tlc.references.get(i), hlRegion);
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
        private ObjectLookupCache cache;
        private boolean caseSensitive;

        protected TablesFinderJob(DBSStructureAssistant structureAssistant, String word, boolean caseSensitive, ObjectLookupCache cache)
        {
            super("Find table names for '" + word + "'", DBIcon.SQL_EXECUTE.getImageDescriptor(), dataSourceProvider.getDataSource());
            this.structureAssistant = structureAssistant;
            this.word = word;
            this.caseSensitive = caseSensitive;
            this.cache = cache;
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            cache.references = new ArrayList<DBSObjectReference>();
            try {
                DBSObjectType[] objectTypes = structureAssistant.getHyperlinkObjectTypes();
                Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(monitor, null, objectTypes, word, caseSensitive, 10);
                if (!CommonUtils.isEmpty(objects)) {
                    cache.references.addAll(objects);
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