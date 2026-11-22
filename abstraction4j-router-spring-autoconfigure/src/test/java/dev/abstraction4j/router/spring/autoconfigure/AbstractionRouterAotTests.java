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

package dev.abstraction4j.router.spring.autoconfigure;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;

import dev.abstraction4j.router.core.ImplementationFinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AOT tests for {@link AbstractionRouterRegistrarPostProcessor}: the context is
 * processed ahead of time, the generated code is compiled, and the router must work in
 * the resulting context.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
@CompileWithForkedClassLoader
class AbstractionRouterAotTests {

	private @Nullable ClassName initializerClassName;

	@Test
	void aotProcessing_registersProxyAndReflectionHintsForRouter() {
		TestGenerationContext generationContext = processAheadOfTime();

		assertThat(
				RuntimeHintsPredicates.proxies().forInterfaces(AopProxyUtils.completeJdkProxyInterfaces(Greeter.class)))
			.accepts(generationContext.getRuntimeHints());
		assertThat(RuntimeHintsPredicates.reflection().onType(Greeter.class))
			.accepts(generationContext.getRuntimeHints());
	}

	@Test
	@SuppressWarnings("unchecked")
	void aotGeneratedContext_routesCalls() {
		TestGenerationContext generationContext = processAheadOfTime();

		TestCompiler.forSystem().with(generationContext).compile((compiled) -> {
			GenericApplicationContext context = new GenericApplicationContext();
			ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
				.getInstance(ApplicationContextInitializer.class, String.valueOf(this.initializerClassName));
			initializer.initialize(context);
			context.refresh();

			assertThat(context.getBeanNamesForType(AbstractionRouterRegistrarPostProcessor.class)).isEmpty();
			assertThat(context.containsBean("greeterFinderRouter")).isTrue();
			Greeter greeter = context.getBean(Greeter.class);
			assertThat(greeter.greet("en")).isEqualTo("hello");
			assertThat(greeter.greet("fr")).isEqualTo("bonjour");
			context.close();
		});
	}

	private TestGenerationContext processAheadOfTime() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(GreeterConfig.class);
		TestGenerationContext generationContext = new TestGenerationContext();
		this.initializerClassName = new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
		generationContext.writeGeneratedContent();
		return generationContext;
	}

	public interface Greeter {

		String greet(String language);

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringAbstraction
	static class GreeterConfig {

		@Bean
		ImplementationFinder<Greeter> greeterFinder() {
			Greeter english = (language) -> "hello";
			Greeter french = (language) -> "bonjour";
			return (Method method, @Nullable Object[] args) -> "fr".equals(args[0]) ? french : english;
		}

	}

}
