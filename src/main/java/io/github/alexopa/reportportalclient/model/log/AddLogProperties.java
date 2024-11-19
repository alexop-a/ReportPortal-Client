/*
 * (C) Copyright 2024 Andreas Alexopoulos (https://alexop-a.github.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.alexopa.reportportalclient.model.log;

import java.util.Date;

import io.github.alexopa.reportportalclient.RPClient;
import lombok.Builder;
import lombok.Getter;

/**
 * Class that models the properties sent to {@link RPClient} to add
 * log to an item
 */
@Builder
@Getter
public class AddLogProperties {
	
	private String launchId;
	private String itemId;
	private String level;
	private Date time;
	private String message;
}