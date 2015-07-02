/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.CommonUtils;

import java.util.*;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = Log.getLog(SQLHyperlinkDetector.class);

    private final SQLEditorBase editor;
    private SQLSyntaxManager syntaxManager;

    private static class ObjectLookupCache {
        List<DBSObjectReference> references;
        boolean loading = true;
    }

    private Map<String, ObjectLookupCache> linksCache = new HashMap<String, ObjectLookupCache>();


    public SQLHyperlinkDetector(SQLEditorBase editor, SQLSyntaxManager syntaxManager)
    {
        this.editor = editor;
        this.syntaxManager = syntaxManager;
    }

    @Nullable
    @Override
    public synchronized IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || editor.getExecutionContext() == null) {
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

        String fullName = wordRegion.identifier;
        String tableName = wordRegion.word;
        boolean caseSensitive = false;
        if (wordDetector.isQuoted(tableName)) {
            tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteSymbol());
            caseSensitive = true;
        }
        String containerName = null;
        if (!CommonUtils.equalObjects(fullName, tableName)) {
            int divPos = fullName.indexOf(syntaxManager.getStructSeparator());
            if (divPos != -1) {
                containerName = fullName.substring(0, divPos);
                if (wordDetector.isQuoted(containerName)) {
                    containerName = DBUtils.getUnQuotedIdentifier(containerName, syntaxManager.getQuoteSymbol());
                }
            }
        }

        if (syntaxManager.getDialect().getKeywordType(fullName) == DBPKeywordType.KEYWORD) {
            // Skip keywords
            return null;
        }
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, editor.getDataSource());
        if (structureAssistant == null) {
            return null;
        }

        ObjectLookupCache tlc = linksCache.get(fullName);
        if (tlc == null) {
            // Start new word finder job
            tlc = new ObjectLookupCache();
            linksCache.put(fullName, tlc);
            TablesFinderJob job = new TablesFinderJob(structureAssistant, containerName, tableName, caseSensitive, tlc);
            job.schedule();
        }
        if (tlc.loading) {
            // Wait for 1000ms maximum
            for (int i = 0; i < 20; i++) {
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
            // Create hyperlinks based on references
            final IRegion hlRegion = new Region(wordRegion.identStart, wordRegion.identEnd - wordRegion.identStart);
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

        private final DBSStructureAssistant structureAssistant;
        private final String containerName;
        private final String word;
        private final ObjectLookupCache cache;
        private final boolean caseSensitive;

        protected TablesFinderJob(DBSStructureAssistant structureAssistant, String containerName, String word, boolean caseSensitive, ObjectLookupCache cache)
        {
            super("Find table names for '" + word + "'", DBeaverIcons.getImageDescriptor(UIIcon.SQL_EXECUTE), editor.getExecutionContext());
            this.structureAssistant = structureAssistant;
            this.containerName = containerName;
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

                DBSObjectContainer container = null;
                if (!CommonUtils.isEmpty(containerName)) {
                    DBSObjectContainer dsContainer = DBUtils.getAdapter(DBSObjectContainer.class, getExecutionContext().getDataSource());
                    if (dsContainer != null) {
                        DBSObject childContainer = dsContainer.getChild(monitor, containerName);
                        if (childContainer instanceof DBSObjectContainer) {
                            container = (DBSObjectContainer) childContainer;
                        } else {
                            // Bad container - stop search
                            return Status.CANCEL_STATUS;
                        }
                    }
                }

                DBSObjectType[] objectTypes = structureAssistant.getHyperlinkObjectTypes();
                Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(monitor, container, objectTypes, word, caseSensitive, 10);
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