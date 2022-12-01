// ============================================================================
//
// Copyright (C) 2006-2022 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.commons.ui.runtime;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.Color;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.talend.commons.exception.ExceptionHandler;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public interface ITalendThemeService {

    public static String DEFAULT_PREFERENCE_ID = "org.eclipse.ui.workbench";

    /**
     * Get color from instance scope preference of default bundleId, which managed by theme; the standard way eclipse
     * uses
     * 
     * @param prop
     * @return the Color, <font color="red">please <b>DON'T</b> dispose it, it is managed by JFaceResources</font>
     */
    static Color getColor(String prop) {
        ITalendThemeService theme = get();
        if (theme != null) {
            return theme.getColorForTheme(DEFAULT_PREFERENCE_ID, prop);
        }
        return null;
    }

    /**
     * Get color from instance scope preference of bundleId, which managed by theme; the standard way eclipse uses
     * 
     * @param bundleId the instance scope preference which stores the prop
     * @param prop
     * @return the Color, <font color="red">please <b>DON'T</b> dispose it, it is managed by JFaceResources</font>
     */
    static Color getColor(String bundleId, String prop) {
        ITalendThemeService theme = get();
        if (theme != null) {
            return theme.getColorForTheme(bundleId, prop);
        }
        return null;
    }

    Color getColorForTheme(String bundleId, String prop);

    /**
     * Get property from instance scope preference of default bundleId, which managed by theme; the standard way eclipse
     * uses
     * 
     * @param key
     * @return
     */
    static String getProperty(String key) {
        ITalendThemeService theme = get();
        if (theme != null) {
            return theme.getPropertyForTheme(DEFAULT_PREFERENCE_ID, key);
        }
        return null;
    }

    /**
     * Get property from instance scope preference of bundleId, which managed by theme; the standard way eclipse uses
     * 
     * @param bundleId the instance scope preference which stores the key
     * @param key
     * @return
     */
    static String getProperty(String bundleId, String key) {
        ITalendThemeService theme = get();
        if (theme != null) {
            return theme.getPropertyForTheme(bundleId, key);
        }
        return null;
    }

    String getPropertyForTheme(String bundleId, String key);

    static void addPropertyChangeListener(IPropertyChangeListener listener) {
        ITalendThemeService theme = get();
        if (theme != null) {
            theme.addPropertyChangeListenerFor(DEFAULT_PREFERENCE_ID, listener);
        }
    }

    static void addPropertyChangeListener(String bundleId, IPropertyChangeListener listener) {
        ITalendThemeService theme = get();
        if (theme != null) {
            theme.addPropertyChangeListenerFor(bundleId, listener);
        }
    }

    void addPropertyChangeListenerFor(String bundleId, IPropertyChangeListener listener);

    static boolean containsPropertyChangeListener(String bundleId, IPropertyChangeListener listener) {
        ITalendThemeService theme = get();
        if (theme != null) {
            return theme.containsPropertyChangeListenerFor(bundleId, listener);
        }
        return false;
    }

    boolean containsPropertyChangeListenerFor(String bundleId, IPropertyChangeListener listener);

    static void removePropertyChangeListener(IPropertyChangeListener listener) {
        ITalendThemeService theme = get();
        if (theme != null) {
            theme.removePropertyChangeListenerFor(DEFAULT_PREFERENCE_ID, listener);
        }
    }

    static void removePropertyChangeListener(String bundleId, IPropertyChangeListener listener) {
        ITalendThemeService theme = get();
        if (theme != null) {
            theme.removePropertyChangeListenerFor(bundleId, listener);
        }
    }

    void removePropertyChangeListenerFor(String bundleId, IPropertyChangeListener listener);

    static ITalendThemeService get() {
        try {
            BundleContext bc = FrameworkUtil.getBundle(ITalendThemeService.class).getBundleContext();
            ServiceReference<ITalendThemeService> serviceReference = bc.getServiceReference(ITalendThemeService.class);
            if (serviceReference == null) {
                return null;
            }
            return bc.getService(serviceReference);
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        return null;
    }

}
