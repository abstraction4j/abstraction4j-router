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

package dev.abstraction4j.router.spring.scanned;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.abstraction4j.router.core.ImplementationFinder;

/**
 * Configuration discovered through component scanning, so its bean definitions are built
 * from ASM metadata rather than reflection.
 */
@Configuration
public class ScannedConfiguration {

	@Bean
	ScannedGreeter english() {
		return (language) -> "hello";
	}

	@Bean
	ScannedGreeter french() {
		return (language) -> "bonjour";
	}

	@Bean
	ImplementationFinder<ScannedGreeter> greeterFinder() {
		ScannedGreeter english = english();
		ScannedGreeter french = french();
		return (method, args) -> "fr".equals(args[0]) ? french : english;
	}

}
