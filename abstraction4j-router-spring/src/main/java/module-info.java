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
 * Abstraction4j routers built on Spring AOP.
 */
@SuppressWarnings("requires-transitive-automatic")
module dev.abstraction4j.router.spring {

	requires transitive dev.abstraction4j.router.core;
	// Routers implement Spring AOP's SpringProxy and Advised, so callers must read spring.aop.
	requires transitive spring.aop;
	requires spring.core;

	requires static org.jspecify;

	exports dev.abstraction4j.router.spring;

}
