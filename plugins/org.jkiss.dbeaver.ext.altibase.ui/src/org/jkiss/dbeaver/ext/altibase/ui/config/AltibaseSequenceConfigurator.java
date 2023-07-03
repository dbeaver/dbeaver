package org.jkiss.dbeaver.ext.altibase.ui.config;

import java.util.Map;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseSequence;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateSequencePage;

public class AltibaseSequenceConfigurator implements DBEObjectConfigurator<AltibaseSequence> {

    @Override
    public AltibaseSequence configureObject(DBRProgressMonitor monitor, Object container, 
            AltibaseSequence sequence, Map<String, Object> options) {
        return new UITask<AltibaseSequence>() {
            @Override
            protected AltibaseSequence runTask() {
                CreateSequencePage editPage = new CreateSequencePage(sequence);
                if (!editPage.edit()) {
                    return null;
                }

                sequence.setName(editPage.getSequenceName());

                return sequence;
            }
        }.execute();
    }

}