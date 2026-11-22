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

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.jdk.JdkAbstractionRouters;
import dev.abstraction4j.router.spring.SpringAbstractionRouters;
import dev.abstraction4j.router.spring.autoconfigure.AbstractionRouterAutoConfiguration;

/**
 * Uses a JDK router, a Spring router and an auto-configured router from a named module,
 * and throws {@link IllegalStateException} if any of them routes a call incorrectly.
 *
 * @author Joe Chambers
 */
@SpringBootApplication(proxyBeanMethods = false)
public class ModulePathApplication {

	private static final Greeter ENGLISH = (language, name) -> "Hello " + name;

	private static final Greeter FRENCH = (language, name) -> "Bonjour " + name;

	/**
	 * Finder for the auto-configured router, registered as {@code greeterFinderRouter}.
	 * @return the finder
	 */
	@Bean
	RoutingImplementationFinder<Greeter> greeterFinder() {
		return finder();
	}

	/**
	 * Routes by the {@link dev.abstraction4j.router.core.RoutingKey @RoutingKey}
	 * parameter when there is one, otherwise by the first {@link String} argument.
	 * @return a new finder
	 */
	static RoutingImplementationFinder<Greeter> finder() {
		return RoutingImplementationFinder.builder(Greeter.class)
			.byRoutingKey(String.class, (language) -> language)
			.or()
			.byArgument(String.class, (language) -> language)
			.route("fr", FRENCH)
			.otherwise(ENGLISH)
			.build();
	}

	/**
	 * Run the checks.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		check(ModulePathApplication.class.getModule().isNamed(), "The application is not in a named module.");
		check(JdkAbstractionRouters.class.getModule().isNamed() && SpringAbstractionRouters.class.getModule().isNamed()
				&& AbstractionRouterAutoConfiguration.class.getModule().isNamed(),
				"The Abstraction4j modules are not on the module path.");

		verify("JdkAbstractionRouters", JdkAbstractionRouters.routerFor(Greeter.class, finder()));
		verify("SpringAbstractionRouters", SpringAbstractionRouters.routerFor(Greeter.class, finder()));
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ModulePathApplication.class)
			.web(WebApplicationType.NONE)
			.bannerMode(Banner.Mode.OFF)
			.run(args)) {
			verify("Auto-configured router", context.getBean(Greeter.class));
		}
		System.out.println("Module path checks passed.");
	}

	private static void verify(String router, Greeter greeter) {
		check("Hello Alice".equals(greeter.greet("en", "Alice")) && "Bonjour Bob".equals(greeter.greet("fr", "Bob"))
		// Routed by the @RoutingKey parameter, which is not the first String.
				&& "Bonjour Carol".equals(greeter.greetIn("Carol", "fr"))
				&& "Hello Dave".equals(greeter.greetIn("Dave", "en")), router + " routed a call incorrectly.");
		System.out.println(router + " works from module " + ModulePathApplication.class.getModule().getName());
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}

}
