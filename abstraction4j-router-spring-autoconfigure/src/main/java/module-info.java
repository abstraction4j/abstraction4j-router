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

/**
 * Spring Boot auto-configuration that registers a router for every
 * {@code ImplementationFinder} bean.
 */
module dev.abstraction4j.router.spring.autoconfigure {

	requires transitive dev.abstraction4j.router.spring;
	requires spring.aop;
	requires spring.beans;
	requires spring.boot.autoconfigure;
	requires spring.context;
	requires spring.core;

	// Spring AOT code generation; only used at build time.
	requires static java.compiler;
	requires static org.jspecify;

	exports dev.abstraction4j.router.spring.autoconfigure;

	// Spring instantiates the package-private registrar and post-processor reflectively.
	opens dev.abstraction4j.router.spring.autoconfigure to spring.beans, spring.context, spring.core;

}
