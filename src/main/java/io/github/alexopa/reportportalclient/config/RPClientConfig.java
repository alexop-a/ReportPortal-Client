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
package io.github.alexopa.reportportalclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.alexopa.reportportalclient.RPClient;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "rp-client")
@Data
public class RPClientConfig {

	private String endpoint;
	private String apiKey;
	private String project;

	@Data
	public static class ConnectionConfig {
		long connectTimeout = 15000L;
		long socketTimeout = 30000L;	
	}
	
	private ConnectionConfig connectionConfig = new ConnectionConfig();
	
	@Bean
	RPClient rpClient() {
		return new RPClient(this);
	}
}
