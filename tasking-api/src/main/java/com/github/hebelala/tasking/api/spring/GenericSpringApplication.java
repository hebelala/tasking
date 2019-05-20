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
package com.github.hebelala.tasking.api.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author hebelala
 */
public class GenericSpringApplication extends AbstractSpringApplication {

	@Override
	public void init() {
		applicationContext = new ClassPathXmlApplicationContext(configLocations());
	}

	@Override
	public void destroy() {
		if (applicationContext != null) {
			((ClassPathXmlApplicationContext) applicationContext).close();
		}
	}

	protected String[] configLocations() {
		return new String[] { "applicationContext.xml" };
	}
}
