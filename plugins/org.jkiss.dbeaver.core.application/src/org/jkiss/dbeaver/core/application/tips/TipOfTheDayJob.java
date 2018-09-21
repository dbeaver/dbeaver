package org.jkiss.dbeaver.core.application.tips;

import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.application.ApplicationWorkbenchAdvisor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.opal.tipoftheday.TipOfTheDay;
import org.osgi.framework.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class TipOfTheDayJob extends AbstractJob {
	private static final Log LOG = Log.getLog(ApplicationWorkbenchAdvisor.class);
    private static final String TIPS_FILE = "tips.xml";

    protected TipOfTheDayJob(String name) {
        super(name);
    }

    public TipOfTheDayJob() {
        super("Tip of the day");
    }


    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
    	List<String> tips = loadTips();
    	if (!tips.isEmpty()) {
    		showTipOfTheDayDialog(tips);
    	}
        return Status.OK_STATUS;
    }

    private void showTipOfTheDayDialog(List<String> tips) {
        UIUtils.asyncExec(() -> {
            final TipOfTheDay tipDialog = new TipOfTheDay();
            
            for (String tip : tips) {
                tipDialog.addTip(tip);
            }
             
            tipDialog.open(UIUtils.getActiveWorkbenchShell());
            DBeaverCore.getGlobalPreferenceStore().
                    setValue(DBeaverPreferences.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP, tipDialog.isShowOnStartup());
        });

    }

    private List<String> loadTips() {
        List<String> result = new ArrayList<>();
		try (InputStream tipsFile = getTipsFile()) {
			JAXBContext jaxbContext = JAXBContext.newInstance(TipsContainer.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	        TipsContainer tips = (TipsContainer) unmarshaller.unmarshal(tipsFile);
	        result.addAll(tips.getTips());
		} catch (JAXBException e) {
			LOG.error("Unable to parse tips file:", e);
		} catch (IOException ioe){
		    LOG.error("TipsContainer file wasn't found", ioe);
        }

		Collections.shuffle(result);
        return result;
    }

    

    private InputStream getTipsFile() throws IOException {
        Bundle plugin = Platform.getBundle ("org.jkiss.dbeaver.core.application");
        URL url = plugin.getEntry (TIPS_FILE);
        //URL resolvedURL = FileLocator.resolve (url);
        return FileLocator.openStream (plugin, new Path(TIPS_FILE), false);
        //return new File (resolvedURL.getFile ());
    }
    
    


}
