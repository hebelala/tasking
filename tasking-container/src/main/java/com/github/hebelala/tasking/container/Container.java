package com.github.hebelala.tasking.container;

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

    private URLClassLoader containerClassLoader;
    private List<URLClassLoader> appClassloaderList = new ArrayList<>();

    public void start() throws MalformedURLException {
        setEnv();
        initClassloader();
    }

    private void setEnv() {

    }

    private void initClassloader() throws MalformedURLException {
        String containerPath = Container.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File containerFile = new File(containerPath);

        // init containerClassloader
        List<URL> containerUrlList = new ArrayList<>();
        loadFiles(containerFile, containerUrlList);
        containerClassLoader = new URLClassLoader(containerUrlList.toArray(new URL[containerUrlList.size()]));

        // init appClassloaderList
        File appsFile = new File(containerFile.getParent(), "apps");
        File[] files = appsFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                List<URL> appUrlList = new ArrayList<>();
                loadFiles(file, appUrlList);
                appClassloaderList.add(new URLClassLoader(appUrlList.toArray(new URL[appUrlList.size()])));
            }
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
