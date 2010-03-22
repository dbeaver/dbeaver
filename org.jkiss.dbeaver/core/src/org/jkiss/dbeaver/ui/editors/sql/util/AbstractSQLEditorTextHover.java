package org.jkiss.dbeaver.ui.editors.sql.util;

import java.util.List;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.keys.KeySequence;
import org.jkiss.dbeaver.core.DBeaverCore;

public abstract class AbstractSQLEditorTextHover implements ITextHover, ITextHoverExtension {

    private static ICommand command;

    static {
        ICommandManager commandManager = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
        command = commandManager.getCommand(".show.sql.info");
        if (!command.isDefined()) {
            command = null;
        }
    }

    /**
     *
     */
    public AbstractSQLEditorTextHover()
    {
    }

    /**
     * Associates a SQL editor with this hover. Subclass can cache it for later use.
     *
     * @param editor
     */
    public abstract void setEditor(IEditorPart editor);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    public IInformationControlCreator getHoverControlCreator()
    {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent)
            {

                int style = SWT.NONE;

                DefaultInformationControl control = new DefaultInformationControl(parent, SWT.RESIZE, style, null,
                                                                                  getTooltipAffordanceString());
                control.setSizeConstraints(60, 10);
                return control;
            }
        }
            ;
    }

    /**
     * Returns the tool tip affordance string.
     *
     * @return the affordance string or <code>null</code> if disabled or no key binding is defined
     * @since 3.0
     */
    protected String getTooltipAffordanceString()
    {
        if (!DBeaverCore.getInstance().getGlobalPreferenceStore().getBoolean("EDITOR_SHOW_TEXT_HOVER_AFFORDANCE"))
        {
            return null;
        }

        KeySequence[] sequences = getKeySequences();
        if (sequences == null) {
            return null;
        }

        String keySequence = sequences[0].format();
        return NLS.bind("SQLErrorHover_makeStickyHint", (new String[]{keySequence}));
    }

    /**
     * Returns the array of valid key sequence bindings for the show tool tip description command.
     *
     * @return the array with the {@link org.eclipse.ui.keys.KeySequence}s
     * @since 3.0
     */
    private KeySequence[] getKeySequences()
    {
        if (command != null) {
            List list = command.getKeySequenceBindings();
            if (!list.isEmpty()) {
                KeySequence[] keySequences = new KeySequence[list.size()];
                for (int i = 0; i < keySequences.length; i++) {
                    keySequences[i] = ((IKeySequenceBinding) list.get(i)).getKeySequence();
                }
                return keySequences;
            }
        }
        return null;
    }

    public IRegion getHoverRegion(ITextViewer textViewer, int offset)
    {
        return SQLWordFinder.findWord(textViewer.getDocument(), offset);
    }

}