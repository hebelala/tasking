/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.actor;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
public class Main {

	public static void main(String[] args) throws Exception {
		List<URL> actorUrlList = new ArrayList<URL>();

		File taFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		File libFile = taFile.getParentFile();
		for (File file : libFile.listFiles()) {
			String name = file.getName();
			if (file.isFile() && name.endsWith(".jar")) {
				actorUrlList.add(file.toURI().toURL());
			} else {
				System.out.println("The file wont be loaded as actor's library: " + name);
			}
		}

		URLClassLoader actorClassLoader = new URLClassLoader(actorUrlList.toArray(new URL[actorUrlList.size()]));
		try {
			actorClassLoader.loadClass("com.github.hebelala.tasking.actor.Actor")
					.getDeclaredMethod("main", String[].class).invoke(null, new Object[] { args });
		} finally {
			actorClassLoader.close();
		}
	}
}
