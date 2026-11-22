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

package dev.abstraction4j.router.jdk;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.core.RoutingKey;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JdkAbstractionRouters}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class JdkAbstractionRoutersTests {

	private final TestAbstraction abstractionA = Mockito.mock(TestAbstraction.class);

	private final TestAbstraction abstractionB = Mockito.mock(TestAbstraction.class);

	private final TestAbstractionFinder finder = Mockito.mock(TestAbstractionFinder.class);

	private final TestAbstraction abstraction = JdkAbstractionRouters.routerFor(TestAbstraction.class, this.finder);

	private final String organizationA = "A";

	private final String organizationB = "B";

	private final Object[] argsA = new Object[] { this.organizationA };

	private final Object[] argsB = new Object[] { this.organizationB };

	private final Method methodToObserve;

	JdkAbstractionRoutersTests() throws Exception {
		this.methodToObserve = TestAbstraction.class.getMethod("doItFor", String.class);
	}

	@Test
	void dynamicJdkAbstraction_simple_handler() throws Exception {
		final var expectedResults = "abstractionA";
		final var expectedAbstraction = this.abstractionA;
		final var expectedNeverAbstraction = this.abstractionB;
		final var expectedMethod = TestAbstraction.class.getMethod("doItFor", String.class);
		final var expectedArg = "A";
		final var expectedArgs = new Object[] { expectedArg };

		willReturn(expectedResults).given(expectedAbstraction).doItFor(eq(expectedArg));

		willReturn(expectedAbstraction).given(this.finder).findImplementation(eq(expectedMethod), eq(expectedArgs));

		String result = this.abstraction.doItFor("A");
		Assertions.assertThat(result).isNotBlank().isEqualTo(expectedResults);

		verify(expectedAbstraction, times(1)).doItFor(eq(expectedArg));
		verify(expectedNeverAbstraction, never()).doItFor(any(String.class));
		verify(this.finder, times(1)).findImplementation(eq(expectedMethod), eq(expectedArgs));
	}

	@Test
	void dynamicJdkAbstraction_simple_handlerB() throws Exception {
		final var expectedResults = "abstractionB";
		final var expectedAbstraction = this.abstractionB;
		final var expectedNeverAbstraction = this.abstractionA;
		final var expectedMethod = TestAbstraction.class.getMethod("doItFor", String.class);
		final var expectedArg = "B";
		final var expectedArgs = new Object[] { expectedArg };

		willReturn(expectedResults).given(expectedAbstraction).doItFor(eq(expectedArg));

		willReturn(expectedAbstraction).given(this.finder).findImplementation(eq(expectedMethod), eq(expectedArgs));

		String result = this.abstraction.doItFor("B");
		Assertions.assertThat(result).isNotBlank().isEqualTo(expectedResults);

		verify(expectedAbstraction, times(1)).doItFor(eq(expectedArg));
		verify(expectedNeverAbstraction, never()).doItFor(any(String.class));
		verify(this.finder, times(1)).findImplementation(eq(expectedMethod), eq(expectedArgs));
	}

	@Test
	void dynamicJdkAbstraction_complex_handlers() throws Exception {
		final var expectedMethod = TestAbstraction.class.getMethod("doItFor", String.class);

		willReturn("abstractionA").given(this.abstractionA).doItFor(eq("A"));

		willReturn("abstractionB").given(this.abstractionB).doItFor(eq("B"));

		willReturn(this.abstractionA).given(this.finder)
			.findImplementation(eq(expectedMethod), eq(new Object[] { "A" }));

		willReturn(this.abstractionB).given(this.finder)
			.findImplementation(eq(expectedMethod), eq(new Object[] { "B" }));

		Assertions.assertThat(this.abstraction.doItFor("A")).isNotBlank().isEqualTo("abstractionA");
		Assertions.assertThat(this.abstraction.doItFor("B")).isNotBlank().isEqualTo("abstractionB");
		Assertions.assertThat(this.abstraction.doItFor("A")).isNotBlank().isEqualTo("abstractionA");
		Assertions.assertThat(this.abstraction.doItFor("B")).isNotBlank().isEqualTo("abstractionB");
		Assertions.assertThat(this.abstraction.doItFor("B")).isNotBlank().isEqualTo("abstractionB");

		verify(this.abstractionA, times(2)).doItFor(eq("A"));
		verify(this.abstractionB, times(3)).doItFor(eq("B"));
		verify(this.finder, times(2)).findImplementation(eq(expectedMethod), eq(new Object[] { "A" }));
		verify(this.finder, times(3)).findImplementation(eq(expectedMethod), eq(new Object[] { "B" }));
	}

	@Test
	void dynamicJdkAbstraction_throwsExceptionWhenFinderReturnsNull() throws Exception {
		var method = TestAbstraction.class.getMethod("doItFor", String.class);

		willReturn(null).given(this.finder).findImplementation(any(), any());

		assertThatThrownBy(() -> this.abstraction.doItFor("anything")).isInstanceOf(IllegalStateException.class)
			.hasMessage("Unable to find implementation for method: <" + method + ">")
			.hasNoCause();
	}

	@Test
	void dynamicJdkAbstraction_throwsExceptionWhenImplementationThrowsException() throws Exception {
		var method = TestAbstraction.class.getMethod("doItFor", String.class);

		willReturn(this.abstractionA).given(this.finder).findImplementation(eq(method), eq(new Object[] { "A" }));

		willThrow(new IllegalArgumentException("necessary must not be null.")).given(this.abstractionA).doItFor("A");

		assertThatThrownBy(() -> this.abstraction.doItFor("A")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("necessary must not be null.")
			.hasNoCause();

		verify(this.abstractionA).doItFor(eq("A"));
		verify(this.abstractionA, never()).doItFor(not(eq("A")));
		verify(this.abstractionB, never()).doItFor(anyString());
		verify(this.finder).findImplementation(eq(method), eq(new Object[] { "A" }));
		verify(this.finder, never()).findImplementation(any(Method.class), not(eq(new Object[] { "A" })));
	}

	@Test
	void toStringVerification() {
		var result = this.abstraction.toString();
		Assertions.assertThat(result)
			.isNotBlank()
			.isEqualTo("Abstraction router for " + TestAbstraction.class.getName() + " using " + this.finder);

		verify(this.finder, never()).findImplementation(any(Method.class), any());
		verify(this.abstractionA, never()).doItFor(any(String.class));
		verify(this.abstractionB, never()).doItFor(any(String.class));
	}

	@Test
	void hashCodeVerification() {
		var result = this.abstraction.hashCode();
		Assertions.assertThat(result).isEqualTo(System.identityHashCode(this.abstraction));

		verify(this.finder, never()).findImplementation(any(Method.class), any());
		verify(this.abstractionA, never()).doItFor(any(String.class));
		verify(this.abstractionB, never()).doItFor(any(String.class));
	}

	@Test
	void equalsVerification_simple() {
		var value = this.abstraction;
		Assertions.assertThat(this.abstraction.equals(value)).isTrue();
	}

	@Test
	void equalsVerification_notEqual() {
		var value = JdkAbstractionRouters.routerFor(TestAbstraction.class, this.finder);

		Assertions.assertThat(this.abstraction.equals(value)).isFalse();
		Assertions.assertThat(this.abstraction.equals(null)).isFalse();
		Assertions.assertThat(this.abstraction.equals("blah")).isFalse();
	}

	@Test
	void methodWithoutParameters_finderReceivesEmptyArguments() {
		Counter counter = mock(Counter.class);
		willReturn(42).given(counter).count();
		Object[][] received = new Object[1][];
		Counter proxy = JdkAbstractionRouters.routerFor(Counter.class, (method, args) -> {
			received[0] = args;
			return counter;
		});

		int result = proxy.count();

		Assertions.assertThat(result).isEqualTo(42);
		Assertions.assertThat(received[0]).isNotNull().isEmpty();
	}

	@Test
	void voidMethod_isInvokedOnImplementation() {
		Counter counter = mock(Counter.class);
		Counter proxy = JdkAbstractionRouters.routerFor(Counter.class, (method, args) -> counter);

		proxy.reset();

		verify(counter).reset();
	}

	@Test
	void nonInterfaceType_throwsIllegalArgumentException() {
		Assertions.assertThatIllegalArgumentException()
			.isThrownBy(() -> JdkAbstractionRouters.routerFor(String.class, (method, args) -> "value"))
			.withMessage("java.lang.String is not an interface.");
	}

	@Test
	void overloadsOfObjectMethodNames_areRoutedToImplementation() {
		Overloads implementation = new Overloads() {

			@Override
			public boolean equals(String other) {
				return true;
			}

			@Override
			public boolean equals(Object first, Object second) {
				return true;
			}

			@Override
			public int hashCode(int seed) {
				return seed;
			}

			@Override
			public String toString(int radix) {
				return "radix " + radix;
			}

		};
		Overloads proxy = JdkAbstractionRouters.routerFor(Overloads.class, (method, args) -> implementation);

		Assertions.assertThat(proxy.equals("other")).isTrue();
		Assertions.assertThat(proxy.equals("first", "second")).isTrue();
		Assertions.assertThat(proxy.hashCode(7)).isEqualTo(7);
		Assertions.assertThat(proxy.toString(16)).isEqualTo("radix 16");
	}

	@Test
	void routingKeyAnnotation_isVisibleThroughTheRouter() {
		Lookup classic = (orderId, customerId) -> "classic";
		Lookup beta = (orderId, customerId) -> "beta";
		Lookup router = JdkAbstractionRouters.routerFor(Lookup.class,
				RoutingImplementationFinder.builder(Lookup.class)
					.byRoutingKey(String.class, (customerId) -> customerId)
					.route("beta-customer", beta)
					.otherwise(classic)
					.build());

		Assertions.assertThat(router.find("order-1", "beta-customer")).isEqualTo("beta");
		Assertions.assertThat(router.find("beta-customer", "other-customer")).isEqualTo("classic");
	}

	@Test
	void invocationHandlerToString_describesRouter() {
		String result = Proxy.getInvocationHandler(this.abstraction).toString();

		Assertions.assertThat(result)
			.isEqualTo("Abstraction router for " + TestAbstraction.class.getName() + " using " + this.finder);
	}

	interface Lookup {

		String find(String orderId, @RoutingKey String customerId);

	}

	interface Overloads {

		boolean equals(String other);

		boolean equals(Object first, Object second);

		int hashCode(int seed);

		String toString(int radix);

	}

	interface Counter {

		int count();

		void reset();

	}

	private interface TestAbstraction {

		String doItFor(String organization);

	}

	private interface TestAbstractionFinder extends ImplementationFinder<TestAbstraction> {

	}

}
