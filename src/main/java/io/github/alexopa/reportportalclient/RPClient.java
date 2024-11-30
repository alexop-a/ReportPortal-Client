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
package io.github.alexopa.reportportalclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.alexopa.reportportalclient.config.RPClientConfig;
import io.github.alexopa.reportportalclient.exception.ReportPortalClientException;
import io.github.alexopa.reportportalclient.model.ReportPortalErrorMessage;
import io.github.alexopa.reportportalclient.model.launch.FinishLaunchProperties;
import io.github.alexopa.reportportalclient.model.launch.StartLaunchProperties;
import io.github.alexopa.reportportalclient.model.log.AddFileAttachmentProperties;
import io.github.alexopa.reportportalclient.model.log.AddLogProperties;
import io.github.alexopa.reportportalclient.model.testitem.FinishTestItemProperties;
import io.github.alexopa.reportportalclient.model.testitem.StartTestItemProperties;
import io.github.alexopa.reportportalclient.rpmodel.EntryCreatedResponse;
import io.github.alexopa.reportportalclient.rpmodel.FinishLaunchRequest;
import io.github.alexopa.reportportalclient.rpmodel.FinishLaunchResponse;
import io.github.alexopa.reportportalclient.rpmodel.FinishTestItemRequest;
import io.github.alexopa.reportportalclient.rpmodel.StartLaunchRequest;
import io.github.alexopa.reportportalclient.rpmodel.StartLaunchResponse;
import io.github.alexopa.reportportalclient.rpmodel.StartTestItemRequest;
import io.github.alexopa.reportportalclient.rpmodel.log.SaveLogRequest;
import io.github.alexopa.reportportalclient.util.AttributeParser;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is a client for ReportPortal. It provides methods to send requests
 * to a ReportPortal instance
 */
@Slf4j
public class RPClient {

	private static final String HEADER_AUTHORIZATION = "Authorization";
	private static final String BEARER_TOKEN = "Bearer ";
	
	private static final String PROJECT_NAME_PATH = "{projectName}";
	private static final String API_PATH = "api/v1";
	private static final String LAUNCH_PATH = "launch";
	private static final String ITEM_PATH = "item";
	private static final String LAUNCH_UUID_PATH = "{launchUuid}";
	private static final String PARENT_UUID_PATH = "{parentUuid}";
	private static final String ITEM_UUID_PATH = "{itemUuid}";
	private static final String FINISH_PATH = "finish";
	private static final String LOG_PATH = "log";
	
	
	private final RestClient client;
	private final String projectName;
	private final String apiKey;

	private final UriComponentsBuilder startLaunchUri;
	private final UriComponentsBuilder finishLaunchUri;
	private final UriComponentsBuilder startItemUri;
	private final UriComponentsBuilder startNestedItemUri;
	private final UriComponentsBuilder finishItemUri;
	private final UriComponentsBuilder addLogUri;

	/**
	 * Creates a new {@link RPClient} instance for a specific project on
	 * ReportPortal
	 * 
	 * @param config A {@link RPClientConfig} with the configuration parameters of the client
	 */
	public RPClient(final RPClientConfig config) {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(
				Collections.singletonList(new MediaType("text", "html", StandardCharsets.UTF_8)));

		List<HttpMessageConverter<?>> c = new ArrayList<>();
		c.add(converter);
		c.add(new MappingJackson2HttpMessageConverter());

		this.client = RestClient.builder().requestFactory(getClientHttpRequestFactory(config))
				.messageConverters(converters -> converters.addAll(c))
				.defaultStatusHandler(new ReportPortalErrorHandler()).build();
		this.projectName = config.getProject();
		this.apiKey = config.getApiKey();

		String endpoint = config.getEndpoint();
		startLaunchUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH,
				LAUNCH_PATH);
		finishLaunchUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH,
				LAUNCH_PATH, LAUNCH_UUID_PATH, FINISH_PATH);
		startItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH, ITEM_PATH);
		startNestedItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH,
				ITEM_PATH, PARENT_UUID_PATH);
		finishItemUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH, ITEM_PATH,
				ITEM_UUID_PATH);
		addLogUri = UriComponentsBuilder.fromHttpUrl(endpoint).pathSegment(API_PATH, PROJECT_NAME_PATH, LOG_PATH);
	}

	private ClientHttpRequestFactory getClientHttpRequestFactory(final RPClientConfig config) {
		final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionConfig().getConnectTimeout()))
				.setSocketTimeout(Timeout.ofMilliseconds(config.getConnectionConfig().getSocketTimeout()))
				.build();
		connManager.setDefaultConnectionConfig(connectionConfig);
		HttpClient httpClient = HttpClientBuilder.create().setConnectionManager(connManager).useSystemProperties()
				.disableRedirectHandling().build();

		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	/**
	 * Starts a new launch on ReportPortal
	 * 
	 * @param props A {@link StartLaunchProperties} object with the properties of
	 *              the launch to start
	 * @return A {@link StartLaunchResponse} object with the response from ReportPortal
	 */
	public StartLaunchResponse startLaunch(StartLaunchProperties props) {
		StartLaunchRequest rq = new StartLaunchRequest();
		rq.setName(props.getName());
		if (StringUtils.isNotBlank(props.getRerunOf())) {
			rq.setRerun(true);
			rq.setRerunOf(props.getRerunOf());
		}
		rq.setStartTime(props.getStartTime());
		Optional.ofNullable(props.getMode()).ifPresent(rq::setMode);
		Optional.ofNullable(props.getDescription()).ifPresent(rq::setDescription);
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));

		ResponseEntity<StartLaunchResponse> rs = client
				.post()
				.uri(startLaunchUri.buildAndExpand(projectName).toUri())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(StartLaunchResponse.class);

		return rs.getBody();
	}

	/**
	 * Finishes a launch on ReportPortal
	 * 
	 * @param props A {@link FinishLaunchProperties} object with the properties of
	 *              the launch to finish
	 * @return A {@link FinishLaunchResponse} object with the response from ReportPortal
	 */
	public FinishLaunchResponse finishLaunch(FinishLaunchProperties props) {
		FinishLaunchRequest rq = new FinishLaunchRequest();
		rq.setEndTime(props.getEndTime());
		Optional.ofNullable(props.getStatus()).ifPresent(rq::setStatus);
		Optional.ofNullable(props.getDescription()).ifPresent(rq::setDescription);
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));
		
		ResponseEntity<FinishLaunchResponse> rs = client
				.put()
				.uri(finishLaunchUri.buildAndExpand(projectName, props.getLaunchUuid()).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(FinishLaunchResponse.class);

		return rs.getBody();
	}

	/**
	 * Starts a new item on ReportPortal
	 * 
	 * @param props A {@link StartTestItemProperties} object with the properties of the
	 *              item to start
	 * @return An {@link EntryCreatedResponse} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedResponse startItem(StartTestItemProperties props) {
		StartTestItemRequest rq = new StartTestItemRequest();
		Optional.ofNullable(props.getDescription()).ifPresent(rq::setDescription);
		Optional.ofNullable(props.getCodeRef()).ifPresent(rq::setCodeRef);
		rq.setName(props.getName());
		rq.setStartTime(props.getStartTime());
		rq.setType(props.getType());
		rq.setLaunchUuid(props.getLaunchUuid());
		Optional.ofNullable(props.getHasStats()).ifPresent(rq::setHasStats);
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));

		URI uri = null;
		if (StringUtils.isBlank(props.getParentUuid())) {
			uri = startItemUri.buildAndExpand(projectName).toUri();
		} else {
			uri = startNestedItemUri.buildAndExpand(projectName, props.getParentUuid()).toUri();
		}

		ResponseEntity<EntryCreatedResponse> rs = client
				.post()
				.uri(uri)
				.accept(MediaType.ALL)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedResponse.class);

		return rs.getBody();
	}

	/**
	 * Finishes an item on ReportPortal
	 * 
	 * @param props A {@link FinishTestItemProperties} object with the properties of the
	 *              item to finish
	 * @return An {@link EntryCreatedResponse} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedResponse finishItem(FinishTestItemProperties props) {
		FinishTestItemRequest rq = new FinishTestItemRequest();
		rq.setEndTime(props.getEndTime());
		rq.setLaunchUuid(props.getLaunchUuid());
		rq.setStatus(props.getStatus());
		Optional.ofNullable(props.getAttributes())
				.ifPresent(attr -> rq.setAttributes(AttributeParser.parseAsSet(attr)));
		
		ResponseEntity<EntryCreatedResponse> rs = client
				.put()
				.uri(finishItemUri.buildAndExpand(projectName, props.getItemUuid()).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedResponse.class);

		return rs.getBody();
	}

	/**
	 * Adds a log message to an item
	 * 
	 * @param props An {@link AddLogProperties} object with the properties of the
	 *              log message to add
	 * @return An {@link EntryCreatedResponse} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedResponse addLog(AddLogProperties props) {
		SaveLogRequest rq = new SaveLogRequest();
		rq.setLaunchUuid(props.getLaunchId());
		rq.setItemUuid(props.getItemId());
		rq.setLevel(props.getLevel());
		rq.setLogTime(props.getTime());
		rq.setMessage(props.getMessage());

		ResponseEntity<EntryCreatedResponse> rs = client
				.post()
				.uri(addLogUri.buildAndExpand(projectName).toUri())
				.accept(MediaType.APPLICATION_JSON)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(rq)
				.retrieve()
				.toEntity(EntryCreatedResponse.class);

		return rs.getBody();
	}

	/**
	 * Adds a file attachment to launch or item
	 * 
	 * @param props An {@link AddFileAttachmentProperties} object with the
	 *              properties of the attachment to add
	 * @return An {@link EntryCreatedResponse} object with the response from
	 *         ReportPortal
	 */
	public EntryCreatedResponse addFileAttachment(AddFileAttachmentProperties props) {
		SaveLogRequest rq = new SaveLogRequest();
		rq.setLaunchUuid(props.getLaunchUuid());
		Optional.ofNullable(props.getItemUuid()).ifPresent(rq::setItemUuid);
		rq.setLevel(props.getLevel());
		rq.setLogTime(props.getTime());
		rq.setMessage(props.getMessage());

		SaveLogRequest.File file = new SaveLogRequest.File();
		file.setName(props.getMessage());
		rq.setFile(file);

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("json_request_part", Arrays.asList(rq));
		parts.add("file", new FileSystemResource(props.getFullPath()));

		ResponseEntity<EntryCreatedResponse> rs = client
				.post()
				.uri(addLogUri.buildAndExpand(projectName).toUri())
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON)
				.header(HEADER_AUTHORIZATION, BEARER_TOKEN + apiKey)
				.body(parts)
				.retrieve()
				.toEntity(EntryCreatedResponse.class);

		return rs.getBody();
	}

	private class ReportPortalErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse response) throws IOException {
			HttpStatusCode statusCode = response.getStatusCode();
			return statusCode.is3xxRedirection() || statusCode.is4xxClientError() || statusCode.is5xxServerError();
		}

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
			if (response.getStatusCode().is3xxRedirection()) {
				throw new ReportPortalClientException(response.getStatusCode(),
						"Redirection responses are not expected. Please check if server is running properly");
			}

			InputStream res = response.getBody();
			ObjectMapper objectMapper = new ObjectMapper();
			ReportPortalErrorMessage errorMessage = null;
			try {
				errorMessage = objectMapper.readValue(res, ReportPortalErrorMessage.class);
			} catch (Exception e) {
				try {
					String error = objectMapper.readValue(res, String.class);
					errorMessage = new ReportPortalErrorMessage();
					errorMessage.setMessage(error);
					errorMessage.setThrowable(e);
				} catch (Exception e1) {
					errorMessage = new ReportPortalErrorMessage();
					errorMessage.setMessage("Failed to parse response as String");
					errorMessage.setThrowable(e1);
				}
			}
			throw new ReportPortalClientException(response.getStatusCode(), errorMessage);
		}
	}
}
