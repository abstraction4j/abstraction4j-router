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

package dev.abstraction4j.router.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers the {@link AbstractionRouterRegistrarPostProcessor} for
 * {@link EnableSpringAbstraction} and {@link AbstractionRouterAutoConfiguration}.
 * Registration is idempotent, so both can be used together.
 *
 * @author Joe Chambers
 */
class AbstractionRouterImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

	static final String POST_PROCESSOR_BEAN_NAME = AbstractionRouterRegistrarPostProcessor.class.getName();

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(POST_PROCESSOR_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.genericBeanDefinition(AbstractionRouterRegistrarPostProcessor.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(POST_PROCESSOR_BEAN_NAME, definition);
		}
	}

}
