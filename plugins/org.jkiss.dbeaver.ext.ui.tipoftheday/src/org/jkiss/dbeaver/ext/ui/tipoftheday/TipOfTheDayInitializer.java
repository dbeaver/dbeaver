package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.core.runtime.Platform;
import org.eclipse.nebula.widgets.opal.tipoftheday.TipOfTheDay;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TipOfTheDayInitializer implements IWorkbenchWindowInitializer {
    private static final Log LOG = Log.getLog(TipOfTheDayInitializer.class);

    @Override
    public void initializeWorkbenchWindow(IWorkbenchWindow window) {
        if (doNotShowTips()){
            return;
        }
        List<String> tips = loadTips();
        if ( !tips.isEmpty()) {
            showTipOfTheDayDialog(tips, window);
        }
    }

    private boolean doNotShowTips() {
        boolean enabled = DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP);
        boolean emptyDatasource = DataSourceRegistry.getAllDataSources().isEmpty();
        return !enabled || emptyDatasource;
    }


    private void showTipOfTheDayDialog(List<String> tips, IWorkbenchWindow window) {
        final TipOfTheDay tipDialog = new TipOfTheDay();

        for (String tip : tips) {
            tipDialog.addTip(tip);
        }

        tipDialog.open(window.getShell());

        DBeaverCore.getGlobalPreferenceStore().
                setValue(DBeaverPreferences.UI_SHOW_TIP_OF_THE_DAY_ON_STARTUP, tipDialog.isShowOnStartup());
    }

    private List<String> loadTips() {
        List<String> result = new ArrayList<>();

        try (InputStream tipsInputStream = getTipsFileStream()) {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            TipsHandler handler = new TipsHandler();
            saxParser.parse(tipsInputStream, handler);
            result.addAll(handler.getTips());

        } catch (SAXException | ParserConfigurationException e) {
            LOG.error("Unable to parse tips file:", e);
        } catch (IOException ioe) {
            LOG.error("Tips file wasn't found", ioe);
        }
        if (!result.isEmpty() && result.size() > 1) {
            Collections.shuffle(result);
        }
        return result;
    }


    private InputStream getTipsFileStream() throws IOException {
        String pathToTipsFile = Platform.getProduct().getProperty("tipsFile");

        URL url = new URL(pathToTipsFile);
        return url.openConnection().getInputStream();
    }

}
