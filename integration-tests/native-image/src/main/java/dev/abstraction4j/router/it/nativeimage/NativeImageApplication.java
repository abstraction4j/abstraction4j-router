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

package dev.abstraction4j.router.it.nativeimage;

import java.util.Map;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.jdk.JdkAbstractionRouters;
import dev.abstraction4j.router.spring.SpringAbstractionRouters;

/**
 * Uses an auto-configured router and routers created manually with
 * {@link JdkAbstractionRouters} and {@link SpringAbstractionRouters}, and throws
 * {@link IllegalStateException} if any of them routes a call incorrectly.
 *
 * @author Joe Chambers
 */
@SpringBootApplication(proxyBeanMethods = false)
@ImportRuntimeHints(RouterHints.class)
public class NativeImageApplication {

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
	 * Router created manually with {@link JdkAbstractionRouters}.
	 * @return the router
	 */
	@Bean
	Greeter jdkGreeter() {
		return JdkAbstractionRouters.routerFor(Greeter.class, finder());
	}

	/**
	 * Router created manually with {@link SpringAbstractionRouters}.
	 * @return the router
	 */
	@Bean
	Greeter springGreeter() {
		return SpringAbstractionRouters.routerFor(Greeter.class, finder());
	}

	/**
	 * Run the checks.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(NativeImageApplication.class)
			.web(WebApplicationType.NONE)
			.bannerMode(Banner.Mode.OFF)
			.run(args)) {
			Map<String, String> routers = Map.of("greeterFinderRouter", "Auto-configured router", "jdkGreeter",
					"JdkAbstractionRouters", "springGreeter", "SpringAbstractionRouters");
			routers.forEach((beanName, router) -> verify(router, context.getBean(beanName, Greeter.class)));
		}
		System.out.println("Native image checks passed.");
	}

	private static void verify(String router, Greeter greeter) {
		boolean routed = "Hello Alice".equals(greeter.greet("en", "Alice"))
				&& "Bonjour Bob".equals(greeter.greet("fr", "Bob"))
				// Routed by the @RoutingKey parameter, which is not the first String.
				&& "Bonjour Carol".equals(greeter.greetIn("Carol", "fr"))
				&& "Hello Dave".equals(greeter.greetIn("Dave", "en"));
		if (!routed) {
			throw new IllegalStateException(router + " routed a call incorrectly.");
		}
		System.out.println(router + " works (" + greeter.getClass().getName() + ")");
	}

}
