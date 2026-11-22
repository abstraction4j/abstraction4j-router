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

package dev.abstraction4j.router.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import dev.abstraction4j.router.benchmarks.Greeter.Language;
import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.jdk.JdkAbstractionRouters;
import dev.abstraction4j.router.spring.SpringAbstractionRouters;

/**
 * Compares a direct call with calls through JDK and Spring routers, using both a fixed
 * finder (router overhead only) and a {@link RoutingImplementationFinder} (router plus
 * typed routing).
 *
 * @author Joe Chambers
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class RouterBenchmarks {

	private static final Greeter ENGLISH = (language, name) -> "Hello " + name;

	private static final Greeter SPANISH = (language, name) -> "Hola " + name;

	/**
	 * Language passed on every call.
	 */
	@Param({ "ENGLISH", "SPANISH" })
	public Language language;

	private Greeter direct;

	private Greeter jdkFixed;

	private Greeter jdkRouted;

	private Greeter springFixed;

	private Greeter springRouted;

	/**
	 * Create the routers under test.
	 */
	@Setup
	public void setUp() {
		this.direct = ENGLISH;
		ImplementationFinder<Greeter> fixed = (method, args) -> ENGLISH;
		ImplementationFinder<Greeter> routed = RoutingImplementationFinder.builder(Greeter.class)
			.byArgument(Language.class, (language) -> language)
			.route(Language.ENGLISH, ENGLISH)
			.route(Language.SPANISH, SPANISH)
			.build();
		this.jdkFixed = JdkAbstractionRouters.routerFor(Greeter.class, fixed);
		this.jdkRouted = JdkAbstractionRouters.routerFor(Greeter.class, routed);
		this.springFixed = SpringAbstractionRouters.routerFor(Greeter.class, fixed);
		this.springRouted = SpringAbstractionRouters.routerFor(Greeter.class, routed);
	}

	/**
	 * Baseline: call the implementation directly.
	 * @return the greeting
	 */
	@Benchmark
	public String direct() {
		return this.direct.greet(this.language, "Alice");
	}

	/**
	 * JDK router with a finder that always returns the same implementation.
	 * @return the greeting
	 */
	@Benchmark
	public String jdkFixed() {
		return this.jdkFixed.greet(this.language, "Alice");
	}

	/**
	 * JDK router with typed routing.
	 * @return the greeting
	 */
	@Benchmark
	public String jdkRouted() {
		return this.jdkRouted.greet(this.language, "Alice");
	}

	/**
	 * Spring router with a finder that always returns the same implementation.
	 * @return the greeting
	 */
	@Benchmark
	public String springFixed() {
		return this.springFixed.greet(this.language, "Alice");
	}

	/**
	 * Spring router with typed routing.
	 * @return the greeting
	 */
	@Benchmark
	public String springRouted() {
		return this.springRouted.greet(this.language, "Alice");
	}

}
