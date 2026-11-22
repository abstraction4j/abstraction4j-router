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

import org.springframework.stereotype.Component;

/**
 * UPS implementation of {@link ShipperService}. A real implementation would call the UPS
 * API here.
 *
 * @author Joe Chambers
 */
@Component
public class UpsShipperService implements ShipperService {

	@Override
	public String ship(Customer customer, String parcel) {
		return "UPS is shipping " + parcel + " for " + customer.name();
	}

}
