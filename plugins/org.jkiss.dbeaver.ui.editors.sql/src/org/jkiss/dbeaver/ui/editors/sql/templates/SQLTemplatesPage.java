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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.templates.AbstractTemplatesPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ProxyPageSite;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorSourceViewer;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorSourceViewerConfiguration;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;


/**
 * The templates page for the SQL editor.
 */
public class SQLTemplatesPage extends AbstractTemplatesPage {

    private static final Log log = Log.getLog(SQLTemplatesPage.class);

    private static final String PREFERENCE_PAGE_ID = "org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage"; //$NON-NLS-1$

    private SQLEditorBase sqlEditor;

    /**
     * Create a new AbstractTemplatesPage for the JavaEditor
     *
     * @param sqlEditor the java editor
     */
    public SQLTemplatesPage(final SQLEditorBase sqlEditor)
    {
        super(sqlEditor, sqlEditor.getViewer());
        this.sqlEditor = sqlEditor;
        IPageSite ps = new ProxyPageSite(sqlEditor.getSite());
        init(ps);
    }

    @Override
    public void insertTemplate(Template template, IDocument document)
    {
        if (!sqlEditor.validateEditorInputState())
            return;

        ISourceViewer contextViewer = sqlEditor.getViewer();
        ITextSelection textSelection = (ITextSelection) contextViewer.getSelectionProvider().getSelection();
        if (!isValidTemplate(document, template, textSelection.getOffset(), textSelection.getLength()))
            return;
        beginCompoundChange(contextViewer);
        /*
           * The Editor checks whether a completion for a word exists before it allows for the template to be
           * applied. We pickup the current text at the selection position and replace it with the first char
           * of the template name for this to succeed.
           * Another advantage by this method is that the template replaces the selected text provided the
           * selection by itself is not used in the template pattern.
           */
        String savedText;
        try {
            savedText = document.get(textSelection.getOffset(), textSelection.getLength());
            if (savedText.length() == 0) {
                String prefix = getIdentifierPart(document, template, textSelection.getOffset(), textSelection.getLength());
                if (prefix.length() > 0 && !template.getName().startsWith(prefix)) {
                    return;
                }
                if (prefix.length() > 0) {
                    contextViewer.setSelectedRange(textSelection.getOffset() - prefix.length(), prefix.length());
                    textSelection = (ITextSelection) contextViewer.getSelectionProvider().getSelection();
                }
            }
            document.replace(textSelection.getOffset(), textSelection.getLength(), template.getName().substring(0, 1));
        } catch (BadLocationException e) {
            endCompoundChange(contextViewer);
            return;
        }
        //Position position = new Position(textSelection.getOffset() + 1, 0);
        Region region = new Region(textSelection.getOffset(), 0);
        textSelection = new TextSelection(textSelection.getOffset(), 1);

        contextViewer.getSelectionProvider().setSelection(textSelection);
        SQLContext context = getContext(document, template, textSelection.getOffset(), textSelection.getLength());

        context.setVariable("selection", savedText); //$NON-NLS-1$
        if (context.getKey().length() == 0) {
            try {
                document.replace(textSelection.getOffset(), 1, savedText);
            } catch (BadLocationException e) {
                endCompoundChange(contextViewer);
                return;
            }
        }
        SQLTemplateCompletionProposal proposal = new SQLTemplateCompletionProposal(template, context, region, null);
        sqlEditor.getSite().getPage().activate(sqlEditor);
        proposal.apply(sqlEditor.getViewer(), ' ', 0, region.getOffset());
        final Point selection = proposal.getSelection(document);
        if (selection != null) {
            sqlEditor.getViewer().setSelectedRange(selection.x, selection.y);
            sqlEditor.getViewer().revealRange(selection.x, selection.y);
        }
        endCompoundChange(contextViewer);
    }

    @Override
    protected ContextTypeRegistry getContextTypeRegistry()
    {
        return SQLTemplatesRegistry.getInstance().getTemplateContextRegistry();
    }

    @Override
    protected IPreferenceStore getTemplatePreferenceStore()
    {
        return new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore());
    }

    @Override
    public TemplateStore getTemplateStore()
    {
        return SQLTemplatesRegistry.getInstance().getTemplateStore();
    }

    @Override
    protected boolean isValidTemplate(IDocument document, Template template, int offset, int length)
    {
        String[] contextIds = getContextTypeIds(document, offset);
        for (String contextId : contextIds) {
            if (contextId.equals(template.getContextTypeId())) {
                DocumentTemplateContext context = getContext(document, template, offset, length);
                return context.canEvaluate(template) || isTemplateAllowed(context, template);
            }
        }
        return false;
    }

    @Override
    protected SourceViewer createPatternViewer(Composite parent)
    {
        IDocument document = new Document();
        SQLEditorSourceViewer viewer = new SQLEditorSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
        SQLEditorSourceViewerConfiguration configuration = new SQLEditorSourceViewerConfiguration(sqlEditor, EditorsPlugin.getDefault().getPreferenceStore());
        viewer.configure(configuration);
        viewer.setEditable(false);
        viewer.setDocument(document);

        Control control = viewer.getControl();
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
        control.setLayoutData(data);

        viewer.setEditable(false);
        return viewer;
    }

    @Override
    protected Image getImage(Template template)
    {
        return DBeaverIcons.getImage(UIIcon.SQL_SCRIPT);
    }

    @Override
    protected void updatePatternViewer(Template template)
    {
        if (template == null) {
            getPatternViewer().getDocument().set(""); //$NON-NLS-1$
            return;
        }
        //String contextId = template.getContextTypeId();

        IDocument doc = getPatternViewer().getDocument();

        String start = ""; //$NON-NLS-1$

        doc.set(start + template.getPattern());
        int startLen = start.length();
        getPatternViewer().setDocument(doc, startLen, doc.getLength() - startLen);
    }

    @Override
    protected String getPreferencePageId()
    {
        return PREFERENCE_PAGE_ID;
    }

    /**
     * Undo manager - end compound change
     *
     * @param viewer the viewer
     */
    private void endCompoundChange(ISourceViewer viewer)
    {
        if (viewer instanceof ITextViewerExtension)
            ((ITextViewerExtension) viewer).getRewriteTarget().endCompoundChange();
    }

    /**
     * Undo manager - begin a compound change
     *
     * @param viewer the viewer
     */
    private void beginCompoundChange(ISourceViewer viewer)
    {
        if (viewer instanceof ITextViewerExtension)
            ((ITextViewerExtension) viewer).getRewriteTarget().beginCompoundChange();
    }

    /**
     * Check whether the template is allowed even though the context can't evaluate it. This is
     * needed because the Dropping of a template is more lenient than ctl-space invoked code assist.
     *
     * @param context  the template context
     * @param template the template
     * @return true if the template is allowed
     */
    private boolean isTemplateAllowed(DocumentTemplateContext context, Template template)
    {
        int offset = context.getCompletionOffset();
        try {
            return template != null && offset > 0 && !isTemplateNamePart(context.getDocument().getChar(offset - 1));
        } catch (BadLocationException e) {
            log.debug(e);
        }
        return false;
    }

    /**
     * Checks whether the character is a valid character in Java template names
     *
     * @param ch the character
     * @return <code>true</code> if the character is part of a template name
     */
    private boolean isTemplateNamePart(char ch)
    {
        return !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != '{' && ch != '}' && ch != ';';
    }

    /**
     * Get context
     *
     * @param document the document
     * @param template the template
     * @param offset   the offset
     * @param length   the length
     * @return the context
     */
    private SQLContext getContext(IDocument document, Template template, final int offset, int length)
    {
        return new SQLContext(
            getContextTypeRegistry().getContextType(template.getContextTypeId()),
            document,
            new Position(offset, length),
            sqlEditor);
    }

    /**
     * Get the active contexts for the given position in the document.
     *
     * @param document the document
     * @param offset   the offset
     * @return an array of valid context id
     */
    @Override
    protected String[] getContextTypeIds(IDocument document, int offset)
    {
        DBPDataSource dataSource = sqlEditor.getDataSource();
        if (dataSource == null) {
            return new String[]{SQLContextTypeBase.ID_SQL};
        } else {
            DBPDriver driver = dataSource.getContainer().getDriver();
            return new String[]{
                SQLContextTypeBase.ID_SQL,
                SQLContextTypeProvider.getTypeId(driver.getProviderId()),
                SQLContextTypeDriver.getTypeId(driver)};
        }
    }

    /**
     * Get the Java identifier terminated at the given offset
     *
     * @param document the document
     * @param template the template
     * @param offset   the offset
     * @param length   the length
     * @return the identifier part the Java identifier
     */
    private String getIdentifierPart(IDocument document, Template template, int offset, int length)
    {
        return getContext(document, template, offset, length).getKey();
    }
}
