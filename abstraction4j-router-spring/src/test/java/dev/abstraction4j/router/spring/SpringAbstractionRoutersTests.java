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

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ClassUtils;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.core.RoutingKey;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringAbstractionRouters}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class SpringAbstractionRoutersTests {

	@MockitoBean
	private TestAbstraction abstractionA;

	@MockitoBean
	private TestAbstraction abstractionB;

	@MockitoBean
	private TestAbstractionFinder finder;

	private TestAbstraction abstraction;

	private final String organizationA = "A";

	private final String organizationB = "B";

	private final String organizationC = "C";

	private final Object[] argsA = new Object[] { this.organizationA };

	private final Object[] argsB = new Object[] { this.organizationB };

	private final Object[] argsC = new Object[] { this.organizationC };

	private final Method methodToObserve = ClassUtils.getMethod(TestAbstraction.class, "doItFor", String.class);

	@BeforeEach
	void setup() {
		this.abstraction = SpringAbstractionRouters.routerFor(TestAbstraction.class, this.finder);
	}

	@Test
	void dynamicSpringAbstractionHandler_simpleImplementationOrgA() {
		willReturn(this.abstractionA).given(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsA));

		willReturn("abstractionA").given(this.abstractionA).doItFor(this.organizationA);

		// execute and validate results.
		Assertions.assertThat(this.abstraction.doItFor(this.organizationA)).isNotBlank().isEqualTo("abstractionA");

		// Positive Verifications
		verify(this.abstractionA).doItFor(eq(this.organizationA));
		verify(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsA));

		// negative verifications
		verify(this.abstractionA, never()).doItFor(not(eq(this.organizationA)));
		verify(this.abstractionB, never()).doItFor(anyString());
		verify(this.finder, never()).findImplementation(eq(this.methodToObserve), not(eq(this.argsA)));
		verify(this.finder, never()).findImplementation(not(eq(this.methodToObserve)), any());
	}

	@Test
	void dynamicSpringAbstractionHandler_simpleImplementationOrgB() {
		willReturn(this.abstractionB).given(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsB));

		willReturn("abstractionB").given(this.abstractionB).doItFor(this.organizationB);

		// Execute and validate results
		Assertions.assertThat(this.abstraction.doItFor(this.organizationB)).isNotBlank().isEqualTo("abstractionB");

		// Positive validations
		verify(this.abstractionB).doItFor(eq(this.organizationB));
		verify(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsB));

		// Negative validations
		verify(this.abstractionB, never()).doItFor(not(eq(this.organizationB)));
		verify(this.abstractionA, never()).doItFor(anyString());
		verify(this.finder, never()).findImplementation(eq(this.methodToObserve), not(eq(this.argsB)));
		verify(this.finder, never()).findImplementation(not(eq(this.methodToObserve)), any());
	}

	@Test
	void dynamicSpringAbstractionHandler_simpleImplementationOrgsAandB() {
		willReturn(this.abstractionA).given(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsA));

		willReturn(this.abstractionB).given(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsB));

		willReturn("abstractionA").given(this.abstractionA).doItFor(this.organizationA);

		willReturn("abstractionB").given(this.abstractionB).doItFor(this.organizationB);

		// Execute and validate results
		Assertions.assertThat(this.abstraction.doItFor(this.organizationA)).isNotBlank().isEqualTo("abstractionA");

		Assertions.assertThat(this.abstraction.doItFor(this.organizationB)).isNotBlank().isEqualTo("abstractionB");

		Assertions.assertThat(this.abstraction.doItFor(this.organizationA)).isNotBlank().isEqualTo("abstractionA");

		Assertions.assertThat(this.abstraction.doItFor(this.organizationB)).isNotBlank().isEqualTo("abstractionB");

		Assertions.assertThat(this.abstraction.doItFor(this.organizationB)).isNotBlank().isEqualTo("abstractionB");

		// Positive validations
		verify(this.abstractionA, times(2)).doItFor(eq(this.organizationA));
		verify(this.abstractionB, times(3)).doItFor(eq(this.organizationB));
		verify(this.finder, times(2)).findImplementation(eq(this.methodToObserve), eq(this.argsA));
		verify(this.finder, times(3)).findImplementation(eq(this.methodToObserve), eq(this.argsB));

		// Negative validations
		verify(this.abstractionA, never()).doItFor(not(eq(this.organizationA)));
		verify(this.abstractionB, never()).doItFor(not(eq(this.organizationB)));
		verify(this.finder, never()).findImplementation(eq(this.methodToObserve),
				not(or(eq(this.argsA), eq(this.argsB))));
		verify(this.finder, never()).findImplementation(not(eq(this.methodToObserve)), any());
	}

	@Test
	void dynamicSpringAbstractionHandler_simpleImplementationOrgC() {
		willReturn(null).given(this.finder).findImplementation(eq(this.methodToObserve), any());

		// Execute and verify exception thrown
		Assertions.assertThatThrownBy(() -> this.abstraction.doItFor(this.organizationC))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Unable to find implementation for method: <" + this.methodToObserve + ">")
			.hasNoCause();

		// Positive validations
		verify(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsC));

		// Negative validations
		verify(this.abstractionA, never()).doItFor(anyString());
		verify(this.abstractionB, never()).doItFor(anyString());
		verify(this.finder, never()).findImplementation(eq(this.methodToObserve), not(eq(this.argsC)));
		verify(this.finder, never()).findImplementation(not(eq(this.methodToObserve)), any());
	}

	@Test
	void dynamicSpringAbstractionHandler_simpleImplementationHandleTargetError() {
		willReturn(this.abstractionA).given(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsA));

		willThrow(new IllegalArgumentException("necessary argument must not be null.")).given(this.abstractionA)
			.doItFor(this.organizationA);

		// Execution and validate exception thrown.
		Assertions.assertThatThrownBy(() -> this.abstraction.doItFor(this.organizationA))
			.hasMessage("necessary argument must not be null.")
			.hasNoCause();

		// Positive verifications
		verify(this.abstractionA).doItFor(eq(this.organizationA));
		verify(this.finder).findImplementation(eq(this.methodToObserve), eq(this.argsA));

		// Negative verifications
		verify(this.abstractionA, never()).doItFor(not(eq(this.organizationA)));
		verify(this.abstractionB, never()).doItFor(anyString());
		verify(this.finder, never()).findImplementation(eq(this.methodToObserve), not(eq(this.argsA)));
		verify(this.finder, never()).findImplementation(not(eq(this.methodToObserve)), any());
	}

	@Test
	void toString_describesRouterWithoutCallingFinder() {
		String result = this.abstraction.toString();

		Assertions.assertThat(result)
			.isEqualTo("Abstraction router for " + TestAbstraction.class.getName() + " using " + this.finder);
		verify(this.finder, never()).findImplementation(any(Method.class), any());
	}

	@Test
	void routingKeyAnnotation_isVisibleThroughTheRouter() {
		Lookup classic = (orderId, customerId) -> "classic";
		Lookup beta = (orderId, customerId) -> "beta";
		Lookup router = SpringAbstractionRouters.routerFor(Lookup.class,
				RoutingImplementationFinder.builder(Lookup.class)
					.byRoutingKey(String.class, (customerId) -> customerId)
					.route("beta-customer", beta)
					.otherwise(classic)
					.build());

		Assertions.assertThat(router.find("order-1", "beta-customer")).isEqualTo("beta");
		Assertions.assertThat(router.find("beta-customer", "other-customer")).isEqualTo("classic");
	}

	@Test
	void proxyConfiguration_describesRouter() {
		String result = ((Advised) this.abstraction).toProxyConfigString();

		Assertions.assertThat(result)
			.contains("Abstraction router for " + TestAbstraction.class.getName() + " using " + this.finder);
	}

	@Test
	void methodWithoutParameters_finderReceivesEmptyArguments() {
		Counter counter = () -> 42;
		Object[][] received = new Object[1][];
		Counter proxy = SpringAbstractionRouters.routerFor(Counter.class, (method, args) -> {
			received[0] = args;
			return counter;
		});

		int result = proxy.count();

		Assertions.assertThat(result).isEqualTo(42);
		Assertions.assertThat(received[0]).isNotNull().isEmpty();
	}

	@Test
	void explicitClassLoader_isUsedForProxyClass() throws Exception {
		Counter counter = () -> 1;
		try (URLClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader())) {
			Counter proxy = SpringAbstractionRouters.routerFor(classLoader, Counter.class, (method, args) -> counter);

			Assertions.assertThat(proxy.getClass().getClassLoader()).isSameAs(classLoader);
		}
	}

	@Test
	void nonInterfaceType_throwsIllegalArgumentException() {
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(() -> SpringAbstractionRouters.routerFor(String.class, (method, args) -> "value"))
			.withMessage("java.lang.String is not an interface.");
	}

	public interface Counter {

		int count();

	}

	interface Lookup {

		String find(String orderId, @RoutingKey String customerId);

	}

	private interface TestAbstraction {

		String doItFor(String organization);

	}

	private interface TestAbstractionFinder extends ImplementationFinder<TestAbstraction> {

	}

}
