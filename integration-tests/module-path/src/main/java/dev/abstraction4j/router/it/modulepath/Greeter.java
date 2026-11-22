/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.abstraction4j.router.it.modulepath;

import dev.abstraction4j.router.core.RoutingKey;

/**
 * Routed interface. Package-private on purpose: the hardest case on the module path.
 *
 * @author Joe Chambers
 */
interface Greeter {

	/**
	 * Greet someone.
	 * @param language the language, which picks the implementation
	 * @param name the name to greet
	 * @return the greeting
	 */
	String greet(String language, String name);

	/**
	 * Greet someone. The language is not the first {@link String} parameter, so routing
	 * on it needs {@link RoutingKey @RoutingKey}.
	 * @param name the name to greet
	 * @param language the language, which picks the implementation
	 * @return the greeting
	 */
	default String greetIn(String name, @RoutingKey String language) {
		return greet(language, name);
	}

}
