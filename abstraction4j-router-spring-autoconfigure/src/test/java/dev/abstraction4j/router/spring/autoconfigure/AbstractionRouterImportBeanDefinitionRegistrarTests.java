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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AbstractionRouterImportBeanDefinitionRegistrar}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class AbstractionRouterImportBeanDefinitionRegistrarTests {

	@Test
	void abstractionRouterImportBeanDefinition_simpleRegistration() {

		final var delegate = new SimpleBeanDefinitionRegistry();
		final var registry = mock(BeanDefinitionRegistry.class, AdditionalAnswers.delegatesTo(delegate));
		final var registrar = new AbstractionRouterImportBeanDefinitionRegistrar();
		final var generator = mock(BeanNameGenerator.class);
		final var metadata = AnnotationMetadata.introspect(SampleAnnotatedConfig.class);

		final var expectedName = AbstractionRouterRegistrarPostProcessor.class.getName();

		registrar.registerBeanDefinitions(metadata, registry, generator);

		final var captor = ArgumentCaptor.forClass(BeanDefinition.class);
		verify(registry).registerBeanDefinition(eq(expectedName), captor.capture());

		final var actual = captor.getValue();

		Assertions.assertThat(actual.getBeanClassName()).isEqualTo(expectedName);
		Assertions.assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);

		final var beanDefinition = registry.getBeanDefinition(expectedName);
		Assertions.assertThat(beanDefinition).isNotNull();
		Assertions.assertThat(beanDefinition.getBeanClassName()).isEqualTo(expectedName);
		Assertions.assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
	}

	@Test
	void abstractionRouterImportBeanDefinition_verifyIdempotency() {
		final var delegate = new SimpleBeanDefinitionRegistry();
		final var registry = mock(BeanDefinitionRegistry.class, AdditionalAnswers.delegatesTo(delegate));
		final var registrar = new AbstractionRouterImportBeanDefinitionRegistrar();
		final var metadata = AnnotationMetadata.introspect(SampleAnnotatedConfig.class);

		final var expectedName = AbstractionRouterRegistrarPostProcessor.class.getName();

		registrar.registerBeanDefinitions(metadata, registry);
		registrar.registerBeanDefinitions(metadata, registry);

		final var captor = ArgumentCaptor.forClass(BeanDefinition.class);
		verify(registry, times(1)).registerBeanDefinition(eq(expectedName), captor.capture());

		final var actual = captor.getValue();

		Assertions.assertThat(actual.getBeanClassName()).isEqualTo(expectedName);
		Assertions.assertThat(actual.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);

		final var beanDefinition = registry.getBeanDefinition(expectedName);
		Assertions.assertThat(beanDefinition).isNotNull();
		Assertions.assertThat(beanDefinition.getBeanClassName()).isEqualTo(expectedName);
		Assertions.assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);

	}

	@EnableSpringAbstraction
	static class SampleAnnotatedConfig {

	}

}
