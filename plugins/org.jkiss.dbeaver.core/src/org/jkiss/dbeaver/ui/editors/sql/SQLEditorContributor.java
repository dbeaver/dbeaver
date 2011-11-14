/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ResourceBundle;

/**
 * SQL Editor contributor
 */
public class SQLEditorContributor extends BasicTextEditorActionContributor
{
    static final String ACTION_CONTENT_ASSIST_PROPOSAL = "ContentAssistProposal"; //$NON-NLS-1$
    static final String ACTION_CONTENT_ASSIST_TIP = "ContentAssistTip"; //$NON-NLS-1$
    static final String ACTION_CONTENT_FORMAT_PROPOSAL = "ContentFormatProposal"; //$NON-NLS-1$
    private boolean isNestedEditor = false;
    private SQLEditorBase activeEditorPart;

    private RetargetTextEditorAction contentAssistProposal;
    private RetargetTextEditorAction contentAssistTip;
    private RetargetTextEditorAction contentFormatProposal;

    public SQLEditorContributor()
    {
        super();

        createActions();
    }

    static String getActionResourcePrefix(String actionId)
    {
        return "actions_" + actionId + "_";//$NON-NLS-1$
    }

    public void setNestedEditor(boolean nestedEditor)
    {
        isNestedEditor = nestedEditor;
    }

    private void createActions()
    {
        // Init custom actions
        ResourceBundle bundle = DBeaverActivator.getResourceBundle();
        contentAssistProposal = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_ASSIST_PROPOSAL));
        contentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        contentFormatProposal = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_FORMAT_PROPOSAL));
        contentFormatProposal.setActionDefinitionId(ICommandIds.CMD_CONTENT_FORMAT);
        contentAssistTip = new RetargetTextEditorAction(bundle, getActionResourcePrefix(ACTION_CONTENT_ASSIST_TIP));
        contentAssistTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
    }

    public void dispose()
    {
        setActiveEditor(null);

        super.dispose();
    }

    public void setActiveEditor(IEditorPart targetEditor)
    {
        super.setActiveEditor(targetEditor);

        if (activeEditorPart == targetEditor) {
            return;
        }
        activeEditorPart = (SQLEditorBase)targetEditor;

        if (activeEditorPart != null) {
            // Update editor actions
            contentAssistProposal.setAction(getAction(activeEditorPart, ACTION_CONTENT_ASSIST_PROPOSAL)); //$NON-NLS-1$
            contentAssistTip.setAction(getAction(activeEditorPart, ACTION_CONTENT_ASSIST_TIP)); //$NON-NLS-1$
            contentFormatProposal.setAction(getAction(activeEditorPart, ACTION_CONTENT_FORMAT_PROPOSAL)); //$NON-NLS-1$
        }
    }

    public void init(IActionBars bars)
    {
        super.init(bars);
    }

    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            //editMenu.add(new Separator());
            editMenu.add(contentAssistProposal);
            editMenu.add(contentAssistTip);
            MenuManager formatMenu = new MenuManager(CoreMessages.actions_menu_edit_ContentFormat);
            editMenu.add(formatMenu);
            formatMenu.add(contentFormatProposal);
            //editMenu.add(new Separator());
            //editMenu.add(executeStatementAction);
            //editMenu.add(executeScriptAction);
        }
    }

    public void contributeToCoolBar(ICoolBarManager manager)
    {
        super.contributeToCoolBar(manager);
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        if (!isNestedEditor) {
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXECUTE_STATEMENT));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXECUTE_SCRIPT));
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_EXPLAIN_PLAN));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_ANALYSE_STATEMENT));
            manager.add(ActionUtils.makeCommandContribution(getPage().getWorkbenchWindow(), ICommandIds.CMD_VALIDATE_STATEMENT));
        }
    }

    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

/*
    private Menu createScriptMenu(final Menu parent, final Shell shell, final SQLEditor editor)
    {
        final Menu menu = parent == null ? new Menu(shell, SWT.POP_UP) : new Menu(parent);
        MenuItem autoCommit = new MenuItem(menu, SWT.PUSH);
        autoCommit.setText("Settings ...");
        autoCommit.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent e)
            {
                DBNNode dsNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(editor.getDataSourceContainer());
                if (dsNode instanceof IAdaptable) {
                    String pageId = PrefPageSQLEditor.PAGE_ID;
                    PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                        shell,
                        (IAdaptable)dsNode,
                        pageId,
                        null,//new String[]{pageId},
                        null);
                    if (propDialog != null) {
                        propDialog.open();
                    }
                }
            }
        });
        return menu;
    }
*/

}
