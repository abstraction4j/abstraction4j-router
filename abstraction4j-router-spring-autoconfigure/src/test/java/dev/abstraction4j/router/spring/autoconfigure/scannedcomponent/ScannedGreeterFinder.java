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

package dev.abstraction4j.router.spring.autoconfigure.scannedcomponent;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.spring.autoconfigure.scanned.ScannedGreeter;

/**
 * Finder discovered as a {@link Component}, so its bean definition starts out holding
 * only the class name.
 */
@Component
public class ScannedGreeterFinder implements ImplementationFinder<ScannedGreeter> {

	private final ScannedGreeter english = (language) -> "hello";

	private final ScannedGreeter french = (language) -> "bonjour";

	@Override
	public ScannedGreeter findImplementation(Method method, @Nullable Object[] args) {
		return "fr".equals(args[0]) ? this.french : this.english;
	}

}
