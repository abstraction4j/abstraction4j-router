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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.core.RoutingImplementationFinder;
import dev.abstraction4j.router.spring.autoconfigure.scanned.ScannedConfiguration;
import dev.abstraction4j.router.spring.autoconfigure.scanned.ScannedGreeter;
import dev.abstraction4j.router.spring.autoconfigure.scannedcomponent.ScannedGreeterFinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Application context tests for {@link AbstractionRouterRegistrarPostProcessor}.
 *
 * @author Joe Chambers
 * @since 0.1.0
 */
class AbstractionRouterRegistrarPostProcessorTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void classBasedFinder_registersPrimaryRouterThatRoutesCalls() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, ClassBasedFinderConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("shipperServiceFinderRouter");

				ShipperService shipperService = context.getBean(ShipperService.class);
				assertThat(AopUtils.isAopProxy(shipperService)).isTrue();
				assertThat(shipperService.ship("ups", "1 Main St")).isEqualTo("ups::<1 Main St>");
				assertThat(shipperService.ship("fedex", "2 Main St")).isEqualTo("fedex::<2 Main St>");
			});
	}

	@Test
	void beanMethodFinder_registersPrimaryRouterThatRoutesCalls() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, BeanMethodFinderConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("shipperServiceFinderRouter");

				ShipperService shipperService = context.getBean(ShipperService.class);
				assertThat(shipperService.ship("ups", "1 Main St")).isEqualTo("ups::<1 Main St>");
				assertThat(shipperService.ship("fedex", "2 Main St")).isEqualTo("fedex::<2 Main St>");
			});
	}

	@Test
	void finderInjectingQualifiedImplementations_routesCalls() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, QualifiedFinderConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();

				ShipperService shipperService = context.getBean(ShipperService.class);
				assertThat(shipperService.ship("ups", "1 Main St")).isEqualTo("ups::<1 Main St>");
				assertThat(shipperService.ship("fedex", "2 Main St")).isEqualTo("fedex::<2 Main St>");
			});
	}

	/**
	 * The router is {@code @Primary}, so an unqualified interface-typed injection point
	 * in the finder resolves to the router, which itself depends on the finder.
	 */
	@Test
	void finderInjectingUnqualifiedInterfaceTypes_failsWithCircularReference() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, UnqualifiedFinderConfig.class)
			.run((context) -> assertThat(context).getFailure()
				.rootCause()
				.isInstanceOf(BeanCurrentlyInCreationException.class));
	}

	@Test
	void componentScannedBeanMethodFinder_registersRouter() {
		this.contextRunner.withUserConfiguration(EnableConfig.class, ScanConfig.class).run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean("greeterFinderRouter");

			ScannedGreeter greeter = context.getBean(ScannedGreeter.class);
			assertThat(greeter.greet("en")).isEqualTo("hello");
			assertThat(greeter.greet("fr")).isEqualTo("bonjour");
		});
	}

	@Test
	void routerIsInjectedIntoBusinessBeans() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, ClassBasedFinderConfig.class,
					OrderServiceConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();

				OrderService orderService = context.getBean(OrderService.class);
				assertThat(orderService.shipperService()).isSameAs(context.getBean("shipperServiceFinderRouter"));
				assertThat(orderService.shipperService().ship("ups", "1 Main St")).isEqualTo("ups::<1 Main St>");
			});
	}

	@Test
	void finderReturningNull_throwsIllegalStateException() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, ClassBasedFinderConfig.class)
			.run((context) -> {
				ShipperService shipperService = context.getBean(ShipperService.class);

				assertThatThrownBy(() -> shipperService.ship("usps", "1 Main St"))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageStartingWith("Unable to find implementation for method:");
			});
	}

	@Test
	void withoutEnableAnnotation_noRouterIsRegistered() {
		this.contextRunner.withUserConfiguration(ShipperImplementations.class, ClassBasedFinderConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean("shipperServiceFinderRouter");
				assertThat(context).doesNotHaveBean(AbstractionRouterRegistrarPostProcessor.class);
				assertThat(context.getBeansOfType(ShipperService.class)).hasSize(2);
			});
	}

	@Test
	void finderForNonInterfaceType_isIgnored() {
		this.contextRunner.withUserConfiguration(EnableConfig.class, ClassTypedFinderConfig.class).run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean("stringFinderRouter");
		});
	}

	@Test
	void multipleFinders_registerOneRouterEach() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, ClassBasedFinderConfig.class,
					ScanConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("shipperServiceFinderRouter");
				assertThat(context).hasBean("greeterFinderRouter");
				assertThat(Proxy.isProxyClass(context.getBean(ScannedGreeter.class).getClass())).isTrue();
			});
	}

	@Test
	void finderClassAnnotatedWithDisableRouter_getsNoRouter() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, DisabledFinderClassConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(DisabledShipperServiceFinder.class);
				assertThat(context).doesNotHaveBean("shipperServiceFinderRouter");
			});
	}

	@Test
	void beanMethodAnnotatedWithDisableRouter_getsNoRouter() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, DisabledBeanMethodConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("shipperServiceFinder");
				assertThat(context).doesNotHaveBean("shipperServiceFinderRouter");
			});
	}

	@Test
	void routingImplementationFinder_registersRouterThatRoutesByArgument() {
		this.contextRunner
			.withUserConfiguration(EnableConfig.class, ShipperImplementations.class, RoutingFinderConfig.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasBean("shipperServiceFinderRouter");

				ShipperService shipperService = context.getBean(ShipperService.class);
				assertThat(shipperService.ship("fedex", "2 Main St")).isEqualTo("fedex::<2 Main St>");
				assertThat(shipperService.ship("anything-else", "3 Main St")).isEqualTo("ups::<3 Main St>");
			});
	}

	@Test
	void componentScannedFinderClass_registersRouter() {
		this.contextRunner.withUserConfiguration(EnableConfig.class, ComponentScanConfig.class).run((context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasBean("scannedGreeterFinderRouter");

			ScannedGreeter greeter = context.getBean(ScannedGreeter.class);
			assertThat(greeter.greet("fr")).isEqualTo("bonjour");
		});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void rawFinder_isIgnored() {
		this.contextRunner.withUserConfiguration(EnableConfig.class)
			.withBean("rawFinder", ImplementationFinder.class, () -> (method, args) -> null)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean("rawFinderRouter");
			});
	}

	@Test
	void registryThatIsNotABeanFactory_throwsIllegalStateException() {
		AbstractionRouterRegistrarPostProcessor postProcessor = new AbstractionRouterRegistrarPostProcessor();

		assertThatIllegalStateException()
			.isThrownBy(() -> postProcessor.postProcessBeanDefinitionRegistry(new SimpleBeanDefinitionRegistry()))
			.withMessageStartingWith("Expected a ConfigurableListableBeanFactory");
	}

	static @Nullable ShipperService route(@Nullable Object[] args, ShipperService ups, ShipperService fedex) {
		if (args == null || args.length == 0) {
			return null;
		}
		return switch ((String) args[0]) {
			case "ups" -> ups;
			case "fedex" -> fedex;
			default -> null;
		};
	}

	public interface ShipperService {

		String ship(String carrier, String address);

	}

	static class UpsService implements ShipperService {

		@Override
		public String ship(String carrier, String address) {
			return "ups::<" + address + ">";
		}

	}

	static class FedexService implements ShipperService {

		@Override
		public String ship(String carrier, String address) {
			return "fedex::<" + address + ">";
		}

	}

	static class ShipperServiceFinder implements ImplementationFinder<ShipperService> {

		private final UpsService ups;

		private final FedexService fedex;

		ShipperServiceFinder(UpsService ups, FedexService fedex) {
			this.ups = ups;
			this.fedex = fedex;
		}

		@Override
		public @Nullable ShipperService findImplementation(Method method, @Nullable Object[] args) {
			return route(args, this.ups, this.fedex);
		}

	}

	@DisableRouter
	static class DisabledShipperServiceFinder extends ShipperServiceFinder {

		DisabledShipperServiceFinder(UpsService ups, FedexService fedex) {
			super(ups, fedex);
		}

	}

	record OrderService(ShipperService shipperService) {
	}

	@Configuration
	@EnableSpringAbstraction
	static class EnableConfig {

	}

	@Configuration
	static class ShipperImplementations {

		@Bean
		UpsService ups() {
			return new UpsService();
		}

		@Bean
		FedexService fedex() {
			return new FedexService();
		}

	}

	@Configuration
	static class ClassBasedFinderConfig {

		@Bean
		ShipperServiceFinder shipperServiceFinder(UpsService ups, FedexService fedex) {
			return new ShipperServiceFinder(ups, fedex);
		}

	}

	@Configuration
	static class BeanMethodFinderConfig {

		@Bean
		ImplementationFinder<ShipperService> shipperServiceFinder(UpsService ups, FedexService fedex) {
			return (method, args) -> route(args, ups, fedex);
		}

	}

	@Configuration
	static class QualifiedFinderConfig {

		@Bean
		ImplementationFinder<ShipperService> shipperServiceFinder(@Qualifier("ups") ShipperService ups,
				@Qualifier("fedex") ShipperService fedex) {
			return (method, args) -> route(args, ups, fedex);
		}

	}

	@Configuration
	static class UnqualifiedFinderConfig {

		@Bean
		ImplementationFinder<ShipperService> shipperServiceFinder(ShipperService ups, ShipperService fedex) {
			return (method, args) -> route(args, ups, fedex);
		}

	}

	@Configuration
	static class ClassTypedFinderConfig {

		@Bean
		ImplementationFinder<String> stringFinder() {
			return (method, args) -> "not an interface";
		}

	}

	@Configuration
	static class DisabledFinderClassConfig {

		@Bean
		DisabledShipperServiceFinder shipperServiceFinder(UpsService ups, FedexService fedex) {
			return new DisabledShipperServiceFinder(ups, fedex);
		}

	}

	@Configuration
	static class DisabledBeanMethodConfig {

		@Bean
		@DisableRouter
		ImplementationFinder<ShipperService> shipperServiceFinder(UpsService ups, FedexService fedex) {
			return (method, args) -> route(args, ups, fedex);
		}

	}

	@Configuration
	static class RoutingFinderConfig {

		@Bean
		RoutingImplementationFinder<ShipperService> shipperServiceFinder(UpsService ups, FedexService fedex) {
			return RoutingImplementationFinder.builder(ShipperService.class)
				.byArgument(String.class, (carrier) -> carrier)
				.route("fedex", fedex)
				.otherwise(ups)
				.build();
		}

	}

	@Configuration
	static class OrderServiceConfig {

		@Bean
		OrderService orderService(ShipperService shipperService) {
			return new OrderService(shipperService);
		}

	}

	@Configuration
	@ComponentScan(basePackageClasses = ScannedConfiguration.class)
	static class ScanConfig {

	}

	@Configuration
	@ComponentScan(basePackageClasses = ScannedGreeterFinder.class)
	static class ComponentScanConfig {

	}

}
