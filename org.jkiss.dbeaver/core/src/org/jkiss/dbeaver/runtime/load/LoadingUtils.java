/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

import net.sf.jkiss.utils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;

import java.lang.reflect.InvocationTargetException;

/**
 * Loading utils
 */
public class LoadingUtils {

    static Log log = LogFactory.getLog(LoadingUtils.class);

    public static Object extractPropertyValue(Object object, String propertyName, ILoadService loadService)
        throws DBException
    {
        // Read property using reflection
        if (object == null) {
            return null;
        }
        Object propertyValue;
        try {
            propertyValue = BeanUtils.readObjectProperty(object, propertyName);
        }
        catch (IllegalAccessException ex) {
            log.warn("Error accessing items " + propertyName, ex);
            return null;
        }
        catch (InvocationTargetException ex) {
            throw new DBException("Error reading items " + propertyName, ex.getTargetException());
        }
        if (propertyValue instanceof ILoadService) {
            // Extract value using nested load service
            ILoadService propService = (ILoadService)propertyValue;
            loadService.setNestedService(propService);
            try {
                propertyValue = propService.evaluate();
            }
            catch (InvocationTargetException ex) {
                throw new DBException("Error loading property: " + propertyName, ex.getTargetException());
            }
            catch (InterruptedException ex) {
                log.info("Property '" + propertyName + "' load interrupted");
                return null;
            }
            finally {
                loadService.clearNestedService();
            }
        }
        return propertyValue;
    }

    public static void executeService(
        ILoadService loadingService,
        ILoadVisualizer visualizer)
    {
        new LoadingJob(loadingService, visualizer).schedule();
    }
}
