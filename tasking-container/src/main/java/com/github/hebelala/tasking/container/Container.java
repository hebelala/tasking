package com.github.hebelala.tasking.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class Container {

    private Logger logger;

    private List<URLClassLoader> appClassloaderList = new ArrayList<>();

    public void start() throws MalformedURLException {
        File libFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        setEnv(libFile);
        initLog();
        initAppClassLoaders(libFile);
    }

    private void setEnv(File libFile) {
        File logsFile = new File(libFile.getParentFile().getParent(), "logs");
        System.setProperty("tasking.log.dir", logsFile.getAbsolutePath());
    }

    private void initLog() {
        logger = LoggerFactory.getLogger(getClass());
    }

    private void initAppClassLoaders(File libFile) throws MalformedURLException {
        File appsFile = new File(libFile.getParent(), "apps");
        if (appsFile.exists()) {
            File[] files = appsFile.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    List<URL> appUrlList = new ArrayList<>();
                    loadFiles(file, appUrlList);
                    appClassloaderList.add(new URLClassLoader(appUrlList.toArray(new URL[appUrlList.size()])));
                }
            }
        } else {
            logger.info("the apps directory does not exists");
        }
    }

    private void loadFiles(File dir, List<URL> urlList) throws MalformedURLException {
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                urlList.add(file.toURI().toURL());
            } else if (file.isDirectory()) {
                loadFiles(file, urlList);
            }
        }
    }

}
