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

package dev.abstraction4j.router.samples.spring;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot sample: the auto-configuration creates the {@link ShipperService} router,
 * and {@link OrderService} receives it by plain constructor injection.
 *
 * @author Joe Chambers
 */
@SpringBootApplication
public class SpringBootSampleApplication {

	/**
	 * Place orders for customers who use different carriers.
	 * @param orderService the order service
	 * @return the runner
	 */
	@Bean
	public ApplicationRunner placeOrders(OrderService orderService) {
		return (args) -> {
			List<Customer> customers = List.of(new Customer("Alice", Carrier.UPS), new Customer("Bob", Carrier.FEDEX),
					new Customer("Carol", Carrier.USPS));
			for (Customer customer : customers) {
				System.out.println(orderService.placeOrder(customer, "2 boxes"));
			}
		};
	}

	/**
	 * Run the sample.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringBootSampleApplication.class, args);
	}

}
