package org.jkiss.dbeaver.ui.editors.sql.scripts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.PropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.commands.DisableEditorServicesHandler;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.Set;


public class ScriptEditorPropertyPage extends PropertyPage {

    private static final Log log = Log.getLog(ScriptEditorPropertyPage.class);

    private static final String DISABLE_EDITOR_SERVICES_PROPERTY = "org.jkiss.dbeaver.ui.editors.sql.scripts.disableEditorServices";
    
    private static final QualifiedName DISABLE_EDITOR_SERVICES_PROP_NAME = new QualifiedName(
        SQLEditorActivator.PLUGIN_ID, DISABLE_EDITOR_SERVICES_PROPERTY
    );

    private Button chkDisableEditorSvcs; 
    
    /**
     * Constructor for SamplePropertyPage.
     */
    public ScriptEditorPropertyPage() {
        super();
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL);
        data.grabExcessHorizontalSpace = true;
        composite.setLayoutData(data);

        chkDisableEditorSvcs = UIUtils.createCheckbox(composite, SQLEditorMessages.sql_editor_prefs_disable_services_text, false);
        chkDisableEditorSvcs.setSelection(getDisableEditorServicesProp(getCurrentFile()));
        chkDisableEditorSvcs.setToolTipText(SQLEditorMessages.sql_editor_prefs_disable_services_tip);
        
        return composite;
    }
    
    /**
     * Returns state of Disable SQL Editor services property
     */
    public static boolean getDisableEditorServicesProp(IFile file) {
        try {
            return CommonUtils.getBoolean(file.getPersistentProperty(DISABLE_EDITOR_SERVICES_PROP_NAME), false);
        } catch (CoreException e) {
            log.debug(e.getMessage(), e);
            return false;            
        }
    }
    
    /**
     * Sets value to Disable SQL Editor services property
     */
    public static void setDisableEditorServicesProp(IFile file, boolean value) throws CoreException {
        file.setPersistentProperty(DISABLE_EDITOR_SERVICES_PROP_NAME, Boolean.toString(value));
        notifyAssociatedServices(file, value);
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
        chkDisableEditorSvcs.setSelection(false);
    }
    
    @Override
    public boolean performOk() {
        try {
            setDisableEditorServicesProp(getCurrentFile(), chkDisableEditorSvcs.getSelection());
        } catch (CoreException e) {
            log.debug(e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    
    private IFile getCurrentFile() {
        return super.getElement().getAdapter(IFile.class);
    }
    
    private static void notifyAssociatedServices(IFile file, boolean newServicesEnabled) {
        Set<DBPPreferenceStore> affectedPrefs = new HashSet<>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorRef : page.getEditorReferences()) {
                    IEditorPart editor = editorRef.getEditor(false);
                    if (editor instanceof SQLEditorBase) {
                        SQLEditorBase sqlEditor = (SQLEditorBase) editor;
                        IFile editorFile = editor.getEditorInput().getAdapter(IFile.class);
                        if (editorFile.equals(file)) {
                            affectedPrefs.add(sqlEditor.getActivePreferenceStore());
                        }
                    }
                }
            }
        }
        for (DBPPreferenceStore prefs : affectedPrefs) {
            notifyPrefs(prefs, newServicesEnabled);
        }

        PlatformUI.getWorkbench().getService(ICommandService.class).refreshElements(DisableEditorServicesHandler.COMMAND_ID, null);
    }
    
    private static void notifyPrefs(DBPPreferenceStore prefStore, boolean newServicesEnabled) {
        final boolean foldingEnabled = prefStore.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED);
        final boolean autoActivationEnabled = prefStore.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        final boolean markWordUnderCursorEnabled = prefStore.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR);
        final boolean markWordForSelectionEnabled = prefStore.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION);
        final boolean oldServicesEnabled = !newServicesEnabled;
        
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.FOLDING_ENABLED,
            oldServicesEnabled && foldingEnabled,
            newServicesEnabled && foldingEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION,
            oldServicesEnabled && autoActivationEnabled,
            newServicesEnabled && autoActivationEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR,
            oldServicesEnabled && markWordUnderCursorEnabled,
            newServicesEnabled && markWordUnderCursorEnabled
        );
        prefStore.firePropertyChangeEvent(
            SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION,
            oldServicesEnabled && markWordForSelectionEnabled,
            newServicesEnabled && markWordForSelectionEnabled
        );
    }
}
