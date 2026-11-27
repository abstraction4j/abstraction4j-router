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

package dev.abstraction4j.router.springboot.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.spring.EnableSpringAbstraction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractionRouterAutoConfiguration}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class AbstractionRouterAutoConfigurationTests {

	// Registered by @EnableSpringAbstraction; the class is package-private in the spring
	// module.
	private static final String POST_PROCESSOR_BEAN_NAME = "dev.abstraction4j.router.spring.AbstractionRouterRegistrarPostProcessor";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AbstractionRouterAutoConfiguration.class))
		.withUserConfiguration(GreeterFinderConfig.class);

	@Test
	void autoConfiguration_isListedInImportsFile() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
			.contains(AbstractionRouterAutoConfiguration.class.getName());
	}

	@Test
	void configurationMetadata_describesEnabledProperty() throws IOException {
		try (InputStream metadata = getClass().getResourceAsStream("/META-INF/spring-configuration-metadata.json")) {
			assertThat(metadata).isNotNull();
			String json = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(json).contains("\"abstraction4j.router.enabled\"", "\"defaultValue\": true");
		}
	}

	@Test
	void autoConfiguration_registersRouterWithoutEnableAnnotation() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean(POST_PROCESSOR_BEAN_NAME);
			assertThat(context).hasBean("greeterFinderRouter");
			assertThat(context.getBean(Greeter.class).greet("fr")).isEqualTo("bonjour");
		});
	}

	@Test
	void autoConfiguration_canBeDisabledByProperty() {
		this.contextRunner.withPropertyValues("abstraction4j.router.enabled=false").run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(POST_PROCESSOR_BEAN_NAME);
			assertThat(context).doesNotHaveBean("greeterFinderRouter");
		});
	}

	@Test
	void autoConfiguration_combinedWithEnableAnnotation_registersPostProcessorOnce() {
		// The runner disallows bean definition overriding, so a second registration
		// fails.
		this.contextRunner.withUserConfiguration(EnableConfig.class).run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean(POST_PROCESSOR_BEAN_NAME);
			assertThat(context.getBean(Greeter.class).greet("en")).isEqualTo("hello");
		});
	}

	interface Greeter {

		String greet(String language);

	}

	@Configuration(proxyBeanMethods = false)
	static class GreeterFinderConfig {

		@Bean
		ImplementationFinder<Greeter> greeterFinder() {
			Greeter english = (language) -> "hello";
			Greeter french = (language) -> "bonjour";
			return (method, args) -> "fr".equals(args[0]) ? french : english;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringAbstraction
	static class EnableConfig {

	}

}
