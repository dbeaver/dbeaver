package org.jkiss.dbeaver.ui.views.console;

import net.sf.jkiss.utils.IntKeyMap;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class ConsoleView extends ViewPart
{
    private Document consoleDocument;
    private TextViewer consoleText;
    private IntKeyMap<Color> colorMap = new IntKeyMap<Color>();
    private Color backgroundColor;

    public void createPartControl(Composite parent)
    {
        Composite top = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 5;
        top.setLayout(layout);

        // Message contents
        consoleText = new TextViewer(top, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.horizontalIndent = 5;
        //gd.verticalIndent = 5;
        consoleText.getControl().setLayoutData(gd);
        
        consoleDocument = new Document();
        consoleText.setDocument(consoleDocument);

        hookContextMenu();
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(consoleText.getControl());
        menuMgr.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                manager.add(new Separator(ITextEditorActionConstants.GROUP_COPY));

                ActionFactory.IWorkbenchAction retargetCopyAction = ActionFactory.COPY.create(ConsoleView.this.getSite().getWorkbenchWindow());
                ActionFactory.IWorkbenchAction retargetSelectAllAction = ActionFactory.SELECT_ALL.create(ConsoleView.this.getSite().getWorkbenchWindow());
                Action copyAction = new Action(retargetCopyAction.getText()) {
                    public void run()
                    {
                        consoleText.doOperation(ITextOperationTarget.COPY);
                    }
                };
                copyAction.setActionDefinitionId(retargetCopyAction.getActionDefinitionId());
                copyAction.setAccelerator(retargetCopyAction.getAccelerator());
                copyAction.setEnabled(consoleText.canDoOperation(ITextOperationTarget.COPY));
                manager.appendToGroup(ITextEditorActionConstants.GROUP_COPY, copyAction);

                Action selectAllAction = new Action(retargetSelectAllAction.getText()) {
                    public void run()
                    {
                        consoleText.doOperation(ITextOperationTarget.SELECT_ALL);
                    }
                };
                selectAllAction.setActionDefinitionId(retargetSelectAllAction.getActionDefinitionId());
                selectAllAction.setAccelerator(retargetSelectAllAction.getAccelerator());
                selectAllAction.setEnabled(consoleText.canDoOperation(ITextOperationTarget.SELECT_ALL));
                manager.appendToGroup(ITextEditorActionConstants.GROUP_COPY, selectAllAction);

                Action clearAction = new Action("Clear console") {
                    public void run()
                    {
                        consoleText.getTextWidget().setText("");
                    }
                };
                clearAction.setEnabled(true);
                manager.appendToGroup(ITextEditorActionConstants.GROUP_COPY, clearAction);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        consoleText.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, consoleText);
    }

    public void setFocus()
    {
    }

    void writeMessage(String text, ConsoleMessageType messageType)
    {
        Color color = colorMap.get(messageType.getColor());
        if (color == null) {
            color = getSite().getShell().getDisplay().getSystemColor(messageType.getColor());
            if (color != null) {
                colorMap.put(messageType.getColor(), color);
            }
        }
        if (backgroundColor == null) {
            backgroundColor = getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE);
        }

        StyledText styledText = consoleText.getTextWidget();
        int caretOffset = styledText.getCaretOffset();
        int textLength = styledText.getCharCount();

        styledText.append(text + "\n");
        StyleRange textStyle = new StyleRange(caretOffset, text.length(), color, backgroundColor);
        styledText.setStyleRange(textStyle);

        if (caretOffset >= textLength) {
            styledText.setSelection(styledText.getCharCount());
        }
    }

}
