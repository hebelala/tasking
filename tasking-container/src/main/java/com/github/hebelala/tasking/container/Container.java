package com.github.hebelala.tasking.container;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class Container {

  private Logger logger;

  private Map<String, URLClassLoader> appClassloaderMap = new HashMap<>();

  public void start() throws MalformedURLException {
    File libFile = new File(
        getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

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
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            String namespace = file.getName();
            logger.info("Found the app, it's namespace is {}", namespace);
            // Support load jar file and classes folder under the root of namespace
            List<URL> appUrlList = new ArrayList<>();
            File[] appFiles = file.listFiles();
            if (appFiles != null) {
              for (File temp : appFiles) {
                appUrlList.add(temp.toURI().toURL());
              }
            }
            appClassloaderMap
                .put(namespace, new URLClassLoader(appUrlList.toArray(new URL[appUrlList.size()])));
          }
        }
      }
    } else {
      logger.info("The apps directory does not exists");
    }
  }

}
