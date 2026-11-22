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

package dev.abstraction4j.router.jdk.fixture;

import org.junit.jupiter.api.Test;

import dev.abstraction4j.router.jdk.JdkAbstractionRouters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link JdkAbstractionRouters} can route a package-private interface declared
 * outside the router's package.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class NonPublicInterfaceJdkTests {

	@Test
	void packagePrivateInterface_routesCalls() {
		Greeter english = (name) -> "hello " + name;
		Greeter proxy = JdkAbstractionRouters.routerFor(Greeter.class, (method, args) -> english);

		String result = proxy.greet("joe");

		assertThat(result).isEqualTo("hello joe");
	}

	interface Greeter {

		String greet(String name);

	}

}
