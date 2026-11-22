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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;

import dev.abstraction4j.router.core.ImplementationFinder;
import dev.abstraction4j.router.spring.SpringAbstractionRouters;

/**
 * {@link BeanDefinitionRegistryPostProcessor} that registers a primary router bean, named
 * {@code <finderBeanName>Router}, for every {@link ImplementationFinder} bean whose
 * target type is an interface. Finders annotated with {@link DisableRouter} are skipped.
 *
 * <p>
 * During Spring AOT processing it also registers the runtime hints a GraalVM native image
 * needs for each router. Because it is a {@link BeanRegistrationAotProcessor}, Spring
 * leaves it out of the AOT-generated context: the router bean definitions are generated
 * at build time instead.
 *
 * @author Joe Chambers
 */
class AbstractionRouterRegistrarPostProcessor
		implements BeanDefinitionRegistryPostProcessor, BeanRegistrationAotProcessor, Ordered {

	/**
	 * Bean definition attribute holding the interface a registered router implements.
	 */
	static final String ROUTED_INTERFACE_ATTRIBUTE = AbstractionRouterRegistrarPostProcessor.class.getName()
			+ ".routedInterface";

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		// Spring always passes its DefaultListableBeanFactory here.
		if (!(registry instanceof ConfigurableListableBeanFactory beanFactory)) {
			throw new IllegalStateException("Expected a ConfigurableListableBeanFactory but got " + registry);
		}

		// Resolve every finder first so the registry isn't modified mid-scan.
		Map<String, Class<?>> routersToRegister = new LinkedHashMap<>();
		// Type prediction also covers @Bean methods read from scanned configuration.
		for (String beanName : beanFactory.getBeanNamesForType(ImplementationFinder.class, true, false)) {
			Class<?> targetInterface = resolveTargetInterface(beanFactory, beanName);
			// Checks both the bean type and the @Bean factory method.
			boolean disabled = beanFactory.findAnnotationOnBean(beanName, DisableRouter.class, false) != null;
			if (targetInterface != null && !disabled) {
				routersToRegister.put(beanName, targetInterface);
			}
		}

		routersToRegister.forEach(
				(finderBeanName, targetInterface) -> registerRouterBean(registry, finderBeanName, targetInterface));
	}

	private @Nullable Class<?> resolveTargetInterface(ConfigurableListableBeanFactory beanFactory, String beanName) {
		// The merged definition holds the return type resolved during prediction.
		ResolvableType beanType = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
		if (beanType.resolve() == null) {
			Class<?> beanClass = beanFactory.getType(beanName, false);
			if (beanClass == null) {
				return null;
			}
			beanType = ResolvableType.forClass(beanClass);
		}

		Class<?> targetType = beanType.as(ImplementationFinder.class).getGeneric(0).resolve();
		return (targetType != null && targetType.isInterface()) ? targetType : null;
	}

	private void registerRouterBean(BeanDefinitionRegistry registry, String finderBeanName, Class<?> targetInterface) {
		RootBeanDefinition routerDefinition = (RootBeanDefinition) BeanDefinitionBuilder
			.rootBeanDefinition(SpringAbstractionRouters.class)
			.setFactoryMethod("routerFor")
			.addConstructorArgValue(targetInterface)
			.addConstructorArgReference(finderBeanName)
			.getBeanDefinition();
		// routerFor is generic and overloaded, so Spring can't predict the router's type.
		routerDefinition.setTargetType(targetInterface);
		routerDefinition.setPrimary(true);
		routerDefinition.setAttribute(ROUTED_INTERFACE_ATTRIBUTE, targetInterface);

		String routerBeanName = finderBeanName + "Router";
		registry.registerBeanDefinition(routerBeanName, routerDefinition);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// Routers are registered in postProcessBeanDefinitionRegistry.
	}

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (!(registeredBean.getMergedBeanDefinition()
			.getAttribute(ROUTED_INTERFACE_ATTRIBUTE) instanceof Class<?> routedInterface)) {
			return null;
		}

		return new BeanRegistrationAotContribution() {

			@Override
			public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
					GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {
				return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {

					// Generate the registration next to the routed interface so it can
					// reference that interface even when it isn't public.
					@Override
					public ClassName getTarget(RegisteredBean registeredBean) {
						return ClassName.get(routedInterface);
					}

				};
			}

			@Override
			public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
				RuntimeHints hints = generationContext.getRuntimeHints();
				// The JDK proxy Spring AOP creates, and the reflective call on to the
				// target.
				hints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(routedInterface));
				hints.reflection().registerType(routedInterface, MemberCategory.INVOKE_PUBLIC_METHODS);
			}

		};
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
