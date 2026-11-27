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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import dev.abstraction4j.router.spring.EnableSpringAbstraction;

/**
 * Spring Boot auto-configuration that registers a router for every
 * {@link dev.abstraction4j.router.core.ImplementationFinder} bean.
 *
 * <p>
 * Enabled by default; set {@code abstraction4j.router.enabled=false} to turn it off. It
 * applies {@link EnableSpringAbstraction}, so applications that don't use Spring Boot get
 * the same routers by adding that annotation themselves.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "abstraction4j.router", name = "enabled", matchIfMissing = true)
@EnableSpringAbstraction
public class AbstractionRouterAutoConfiguration {

	/**
	 * Create a new instance. Called by Spring Boot.
	 */
	public AbstractionRouterAutoConfiguration() {
	}

}
