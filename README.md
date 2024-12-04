# ReportPortal Client

**reportportal-client** is a Java client for interacting with ReportPortal's REST api. In this version it offers only some basic api calls for launches and items, but it can be enhanced if needed.

## Installation
Add the following dependency in pom.xml file
```
<dependency>
	<artifactId>reportportal-rest-client</artifactId>
	<groupId>io.github.alexop-a</groupId>
	<version>1.0.1</version>
</dependency>
 ```
## Initialization

The client can be initialized either as a spring Bean or as a simple java class. 

### Bean Initialization

- Add the `config` package in the component-scan of your project, ie:
```
@ComponentScan("io.github.alexopa.reportportalclient.config")
```

- Autowire the client and start using it
```
@Autowired
RPClient rpClient;

rpClient.startLaunch(StartLaunchProperties.builder()....build());
```

- The client is configured via the properties in `RPClientConfig` class. Those properties can be added in any properties file that is available in your spring project.
```
rp-client.api-key=test_12345
rp-client.endpoint=http://localhost:8080/
rp-client.project=superadmin_personal
rp-client.connection-config.connect-timeout=30000
rp-client.connection-config.socket-timeout=30000
```

### Java class initialization

In case of initializing the client as a java class, an `RPClientConfig` object needs to be created with the configuration options, like this:
```
RPClientConfig rpClientConfig = new RPClientConfig();
rpClientConfig.setEndpoint("http://localhost:8080/");
rpClientConfig.setApiKey("test_12345");
rpClientConfig.setProject("superadmin_personal");

RPClient rpClient = new RPClient(rpClientConfig);
```

## Usage

Each request to the rest api requires a properties objects with the parameters required for the call to api. For example, in order to start a new launch the method is:
```
public StartLaunchResponse startLaunch(LaunchProperties props)
```
The methods do not do any validation on the properties passed to api, ie. they do not check if all required fields are set or not, so it is up to the user to pass the expected parameters.
