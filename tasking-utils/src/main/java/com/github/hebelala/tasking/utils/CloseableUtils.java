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
package com.github.hebelala.tasking.utils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author hebelala
 */
public final class CloseableUtils {

	public static void closeQuietly(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Invoke <code>close</code> method by reflection of <code>getMethod()</code>.
	 */
	public static void closeQuietly(Object object) {
		if (object == null) {
			return;
		}
		try {
			object.getClass().getMethod("close").invoke(object);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
		}
	}
}
