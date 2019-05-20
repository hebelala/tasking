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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author hebelala
 */
public final class CollectionUtils {

	public static <E> boolean equals(List<E> l, List<E> l2) {
		if (l == l2) {
			return true;
		}
		if (l == null || l2 == null) {
			return false;
		}
		int size = l.size();
		if (size != l2.size()) {
			return false;
		}
		for (int i = 0; i < size; i++) {
			if (!Objects.equals(l.get(i), l2.get(i))) {
				return false;
			}
		}
		return true;
	}

	public static <E> boolean notContains(List<E> l, E e) {
		return l == null || !l.contains(e);
	}

	public static <E> boolean isNotBlank(Collection<E> c) {
		return c != null && !c.isEmpty();
	}
}
