package org.talend.signon.util;

// ============================================================================
//
// Copyright (C) 2006-2021 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.talend.utils.io.FilesUtils;

public class SignClientInstallService {

    private static Logger LOGGER = Logger.getLogger(SignClientInstallService.class);

    private final String SIGN_CLIENT_BUNDLE_NAME = "org.talend.singlesignon.client";
    private final String installFileName = "TalendSignTool.zip";
    
    private final String version = "8.0.1.202205101138";

    private static final SignClientInstallService instance = new SignClientInstallService();

    public static SignClientInstallService getInstance() {
        return instance;
    }

    private SignClientInstallService() {

    }

    public boolean isNeedInstall() {
        if (EnvironmentUtils.isMacOsSytem()) {
            return false;
        }
        String installedVersion = getInstalledVersion();
        if (installedVersion != null && installedVersion.compareTo(version) >= 0) {
            return false;
        }
        return true;
    }

    public void install() throws Exception {
        File sourceFile = getInstallFile();
        if (!sourceFile.exists()) {
            LOGGER.error("Can't find install file:" + sourceFile.getAbsolutePath());
        }
        File targetFolder = getInstallDir();
        if (targetFolder.exists()) {
            targetFolder.delete();
            LOGGER.info("Deleted target folder:" + targetFolder.getAbsolutePath());
        }
        targetFolder.mkdirs();
        LOGGER.info("Created target folder:" + targetFolder.getAbsolutePath());
        FilesUtils.unzip(sourceFile.getAbsolutePath(), targetFolder.getAbsolutePath(), true);
        LOGGER.info("Installed sign client:" + targetFolder.getAbsolutePath());
    }

    private String getInstalledVersion() {
        FileInputStream in = null;
        try {
            File eclipseProductFile = getEclipseProductFile();
            if (eclipseProductFile != null && eclipseProductFile.exists()) {
                Properties p = new Properties();
                in = new FileInputStream(eclipseProductFile);
                p.load(in);
                String productFileVersion = p.getProperty("version"); //$NON-NLS-1$
                return productFileVersion;
            }
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
        return null;
    }

    private File getEclipseProductFile() throws URISyntaxException {
        File eclipseproductFile = new File(getInstallDir(), ".eclipseproduct");//$NON-NLS-1$
        return eclipseproductFile;
    }
    
    protected File getInstallDir() {
        return SignOnClientUtil.getSignToolFolder();
    }
    
    protected File getInstallFile() throws IOException {
        BundleContext context = getCurrentBundleContext();
        Bundle[] bundles = context.getBundles();
        Bundle bundle = null;
        for (Bundle b : bundles) {
            if (SIGN_CLIENT_BUNDLE_NAME.equals(b.getSymbolicName())) {
                bundle = b;
                break;
            }
        }
        if (bundle != null) {
            File bundleFile = FileLocator.getBundleFile(bundle).getAbsoluteFile();
            File folder = new File (bundleFile, "repository");
            File installFile = new File(folder, installFileName);
            return installFile;
        }
        return null;
    }
    
    // always return a valid bundlesContext or throw a runtimeException
    public static BundleContext getCurrentBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(SignClientInstallService.class);
        if (bundle != null) {
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext != null) {
                return bundleContext;
            } else {
                throw new RuntimeException(
                        "could not find current BundleContext, this should never happen, check that the bunlde is activated when this class is accessed");
            }
        } else {
            throw new RuntimeException(
                    "could not find current Bundle, this should never happen, check that the bunlde is activated when this class is accessed");
        }
    }
}
