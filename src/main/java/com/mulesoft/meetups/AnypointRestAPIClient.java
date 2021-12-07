package com.mulesoft.meetups;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnypointRestAPIClient {

	private static final String ANYPOINT_LOGIN_URL = "https://anypoint.mulesoft.com/accounts/login";
	private static final String ANYPOINT_CURRENT_USER_URL = "https://anypoint.mulesoft.com/accounts/api/me";
	private static final String ANYPOINT_ENVIRONMENTS_URL = "https://anypoint.mulesoft.com/apimanager/xapi/v1/organizations/%s/environments?withCloudhubPermissions=false";
	private static final String ANYPOINT_API_VERSIONS_URL = "https://anypoint.mulesoft.com/apimanager/xapi/v1/organizations/%s/exchangeAssets?assetId=%s&groupId=%s";
	private static final String ANYPOINT_API_SLA_TIERS_URL = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations/%s/environments/%s/apis/%s/tiers";
	private static final String ANYPOINT_API_SLA_TIER_URL = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations/%s/environments/%s/apis/%s/tiers/%s";
	private static final String ANYPOINT_API_CLIENT_CONTRACTS_URL = "https://anypoint.mulesoft.com/exchange/api/v2/organizations/%s/applications/%s/contracts";
	private static final String ANYPOINT_API_CLIENT_CONTRACT_URL = "https://anypoint.mulesoft.com/apimanager/api/v1/organizations/%s/environments/%s/apis/%s/contracts/%s";
	private static final String ANYPOINT_API_CLIENT_CONTRACT_REVOKE_URL = "https://anypoint.mulesoft.com/apimanager/xapi/v1/organizations/%s/environments/%s/apis/%s/contracts/%s/revoke";
	private static final String ANYPOINT_API_CLIENT_APPLICATIONS_URL = "https://anypoint.mulesoft.com/exchange/api/v2/organizations/%s/applications";
	private static final String ANYPOINT_API_LIST_BY_ENVIRONMENT_ID_URL = "https://anypoint.mulesoft.com/apimanager/xapi/v1/organizations/%s/environments/%s/apis?sort=name&ascending=true";
	private static final String ANYPOINT_CLIENT_APPLICATIONS_URL = "https://anypoint.mulesoft.com/exchange/api/v2/organizations/%s/applications";
	private static final String ANYPOINT_ACCESS_TOKEN_PROPERTY = "access_token";
	private static final String ANYPOINT_TOKEN_TYPE_PROPERTY = "token_type";
	private static final String ANYPOINT_REDIRECT_URL_PROPERTY = "redirectUrl";
	private static final String ANYPOINT_AUTHORIZATION_HEADER = "Authorization";
	private static final String ANYPOINT_AUTHORIZATION_BEARER = "Bearer %s";
	private static final String ANYPOINT_MASTER_ORGANIZATION_ID_PROPERTY = "masterOrganizationId";
	private static final String ANYPOINT_URL_PROPERTY = "url";
	private static final String ANYPOINT_DESCRIPTION_PROPERTY = "description";
	private static final String ANYPOINT_ID_PROPERTY = "id";
	private static final String ANYPOINT_API_DEFINITIONS_PROPERTY = "apiDefinitions";
	private static final String ANYPOINT_STATUS_PROPERTY = "status";
	private static final String ANYPOINT_TYPE_PROPERTY = "type";
	private static final String ANYPOINT_ORGANIZATION_ID_PROPERTY = "organizationId";
	private static final String ANYPOINT_NAME_PROPERTY = "name";
	private static final String ANYPOINT_PRODUCT_API_VERSION_PROPERTY = "productAPIVersion";
	private static final String ANYPOINT_ASSET_ID_PROPERTY = "assetId";
	private static final String ANYPOINT_GROUP_ID_PROPERTY = "groupId";
	private static final String ANYPOINT_VERSION_PROPERTY = "version";
	private static final String ANYPOINT_ASSET_VERSION_PROPERTY = "assetVersion";
	private static final String ANYPOINT_ENVIRONMENT_ID_PROPERTY = "environmentId";
	private static final String ANYPOINT_CLIENT_ID_PROPERTY = "clientId";
	private static final String ANYPOINT_ENVIRONMENTS_PROPERTY = "environments";
	private static final String ANYPOINT_ASSET_PROPERTY = "asset";
	private static final String ANYPOINT_EXCHANGE_ASSET_NAME = "exchangeAssetName";
	private static final String ANYPOINT_INSTANCES_PROPERTY = "instances";
	private static final String ANYPOINT_USER_PROPERTY = "user";
	private static final String ANYPOINT_FIRST_NAME_PROPERTY = "firstName";
	private static final String ANYPOINT_LAST_NAME_PROPERTY = "lastName";
	private static final String ANYPOINT_EMAIL_PROPERTY = "email";
	private static final String ANYPOINT_PHONE_NUMBER_PROPERTY = "phoneNumber";
	private static final String ANYPOINT_USERNAME_PROPERTY = "username";
	private static final String ANYPOINT_CLIENT_SECRET_PROPERTY = "clientSecret";

	/**
	 * Get an authentication token in Anypoint Platform.
	 * @param login Anypoint credentials
	 * @return Anypoint access token
	 */
	public AnypointToken getToken(AnypointLogin login) {

		Map<String, String> response = WebClient.builder().build().post().uri(ANYPOINT_LOGIN_URL)
				.body(BodyInserters.fromValue(login)).retrieve().bodyToMono(Map.class).block();

		return AnypointToken.builder()
				.accessToken(response.get(ANYPOINT_ACCESS_TOKEN_PROPERTY))
				.tokenType(response.get(ANYPOINT_TOKEN_TYPE_PROPERTY))
				.redirectUrl(response.get(ANYPOINT_REDIRECT_URL_PROPERTY))
				.build();
	}

	/**
	 * Gets a list of client applications in Anypoint Exchange.
	 * @param accessToken Anypoint access token
	 * @param groupId Anypoint group id
	 * @return List of Anypoint Exchange client applications
	 */
	public List<AnypointExchangeClientApplication> getClientApplicationsInExchange(String accessToken, String groupId) {

		List<Map> response = WebClient.builder().build().get()
				.uri(String.format(ANYPOINT_CLIENT_APPLICATIONS_URL, groupId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(List.class).block();

		return response.stream()
				.map(
						a -> AnypointExchangeClientApplication.builder()
								.masterOrganizationId(a.get(ANYPOINT_MASTER_ORGANIZATION_ID_PROPERTY).toString())
								.url(("" + a.getOrDefault(ANYPOINT_URL_PROPERTY, "")))
								.description(("" + a.getOrDefault(ANYPOINT_DESCRIPTION_PROPERTY, "")))
								.id(Long.parseLong(("" + a.getOrDefault(ANYPOINT_ID_PROPERTY, Long.MIN_VALUE))))
								.build()
				)
				.collect(Collectors.toList());
	}

	/**
	 *
	 * @param accessToken
	 * @param groupId
	 * @param environmentId
	 * @param apiId
	 * @param slaTierId
	 */
	public void deleteAPISlaTier(String accessToken, String groupId, String environmentId, Long apiId, Long slaTierId) {
		Map response = WebClient.builder().build().delete()
				.uri(String.format(ANYPOINT_API_SLA_TIER_URL, groupId, environmentId, apiId, slaTierId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();
	}

	/**
	 *
	 * @param accessToken
	 * @param groupId
	 * @param environmentId
	 * @param apiId
	 * @param slaTier
	 */
	public Long createAPISlaTier(String accessToken, String groupId, String environmentId, Long apiId, AnypointAPISlaTier slaTier) {
		Map response = WebClient.builder().build().post()
				.uri(String.format(ANYPOINT_API_SLA_TIERS_URL, groupId, environmentId, apiId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.body(BodyInserters.fromValue(slaTier)).retrieve().bodyToMono(Map.class).block();

		return Long.parseLong(response.getOrDefault(ANYPOINT_ID_PROPERTY, Long.MIN_VALUE).toString());
	}

	/**
	 *
	 * @param accessToken
	 * @param groupId
	 * @param apiId
	 * @param contractId
	 * @throws JsonProcessingException
	 */
	public void deleteAPIClientContract(String accessToken, String groupId, String environmentId, Long apiId, Long contractId) throws JsonProcessingException {

		WebClient.builder().build()
				.post()
				.uri(String.format(ANYPOINT_API_CLIENT_CONTRACT_REVOKE_URL, groupId, environmentId, apiId, contractId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();

		WebClient.builder().build()
				.delete()
				.uri(String.format(ANYPOINT_API_CLIENT_CONTRACT_URL, groupId, environmentId, apiId, contractId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();
	}

	/**
	 *
	 * @param accessToken
	 * @param groupId
	 * @param applicationId
	 * @param contract
	 */
	public Long createAPIClientContract(String accessToken, String groupId, Long applicationId, AnypointAPIContract contract) throws JsonProcessingException {

		Map response = WebClient.builder().build()
				.post()
				.uri(String.format(ANYPOINT_API_CLIENT_CONTRACTS_URL, groupId, applicationId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.body(BodyInserters.fromValue(contract))
				.retrieve().bodyToMono(Map.class).block();

		return Long.parseLong(response.getOrDefault(ANYPOINT_ID_PROPERTY, Long.MIN_VALUE).toString());
	}

	/**
	 *
	 * @param accessToken
	 * @param groupId
	 * @param clientApplication
	 * @return
	 */
	public Long createAPIClientApplication(String accessToken, String groupId, AnypointExchangeClientApplication clientApplication) {

		Map response = WebClient.builder().build()
				.post()
				.uri(String.format(ANYPOINT_API_CLIENT_APPLICATIONS_URL, groupId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.body(BodyInserters.fromValue(clientApplication))
				.retrieve().bodyToMono(Map.class).block();

		clientApplication.setClientId(response.get(ANYPOINT_CLIENT_ID_PROPERTY).toString());
		clientApplication.setClientSecret(response.get(ANYPOINT_CLIENT_SECRET_PROPERTY).toString());
		clientApplication.setId(Long.parseLong(response.getOrDefault(ANYPOINT_ID_PROPERTY, Long.MIN_VALUE).toString()));
		clientApplication.setMasterOrganizationId(response.get(ANYPOINT_MASTER_ORGANIZATION_ID_PROPERTY).toString());

		return Long.parseLong(response.getOrDefault(ANYPOINT_ID_PROPERTY, Long.MIN_VALUE).toString());
	}

	/**
	 * Gets details of currently logged user.
	 * @param accessToken Anypoint access token
	 * @return Anypoint user details
	 */
	public AnypointUser getUser(String accessToken) {

		Map response = WebClient.builder().build()
				.get()
				.uri(ANYPOINT_CURRENT_USER_URL)
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();

		Map anypointUser = (Map) response.get(ANYPOINT_USER_PROPERTY);

		return AnypointUser.builder()
				.id(anypointUser.get(ANYPOINT_ID_PROPERTY).toString())
				.organizationId(anypointUser.get(ANYPOINT_ORGANIZATION_ID_PROPERTY).toString())
				.firstName(anypointUser.get(ANYPOINT_FIRST_NAME_PROPERTY).toString())
				.lastName(anypointUser.get(ANYPOINT_LAST_NAME_PROPERTY).toString())
				.email(anypointUser.get(ANYPOINT_EMAIL_PROPERTY).toString())
				.phoneNumber(anypointUser.get(ANYPOINT_PHONE_NUMBER_PROPERTY).toString())
				.username(anypointUser.get(ANYPOINT_USERNAME_PROPERTY).toString())
				.build();
	}

	/**
	 * Gets a list of APIs by environment ID
	 * @param accessToken Anypoint access token
	 * @param groupId Anypoint group ID
	 * @param environmentId Anypoint environment ID
	 * @return List of APIs
	 */
	public List<AnypointAPI> getAPIsByEnvironmentId(String accessToken, String groupId, String environmentId) {

		Map response = WebClient.builder().build().get().uri(
				String.format(ANYPOINT_API_LIST_BY_ENVIRONMENT_ID_URL, groupId, environmentId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken)).retrieve().bodyToMono(Map.class).block();

		return ((List<Map>) response.get(ANYPOINT_INSTANCES_PROPERTY)).stream().map(a ->
				AnypointAPI.builder()
						.assetId(a.get(ANYPOINT_ASSET_ID_PROPERTY).toString())
						.assetVersion(a.get(ANYPOINT_ASSET_VERSION_PROPERTY).toString())
						.environmentId(a.get(ANYPOINT_ENVIRONMENT_ID_PROPERTY).toString())
						.id(Long.parseLong(a.get(ANYPOINT_ID_PROPERTY).toString()))
						.assetId(((Map)a.get(ANYPOINT_ASSET_PROPERTY)).get(ANYPOINT_ASSET_ID_PROPERTY).toString())
						.asset(
								AnypointAPIAsset.builder()
										.exchangeAssetName(((Map)a.get(ANYPOINT_ASSET_PROPERTY)).get(ANYPOINT_EXCHANGE_ASSET_NAME).toString())
										.name(((Map)a.get(ANYPOINT_ASSET_PROPERTY)).get(ANYPOINT_NAME_PROPERTY).toString())
										.build())
						.build())
				.collect(Collectors.toList());
	}

	/**
	 * Gets a list of environments in Anypoint Platform.
	 * @param accessToken Anypoint access token
	 * @param groupId Anypoint group ID
	 * @return List of Anypoint environments
	 */
	public List<AnypointEnvironment> getEnvironments(String accessToken, String groupId) {

		Map<String, List<Map<String, String>>> response = WebClient.builder().build()
				.get()
				.uri(String.format(ANYPOINT_ENVIRONMENTS_URL, groupId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();

		return response.get(ANYPOINT_ENVIRONMENTS_PROPERTY).stream().map(d ->
				AnypointEnvironment.builder()
						.clientId(d.get(ANYPOINT_CLIENT_ID_PROPERTY))
						.id(d.get(ANYPOINT_ID_PROPERTY))
						.name(d.get(ANYPOINT_NAME_PROPERTY))
						.organizationId(d.get(ANYPOINT_ORGANIZATION_ID_PROPERTY))
						.type(d.get(ANYPOINT_TYPE_PROPERTY))
						.build()
		)
				.collect(Collectors.toList());
	}

	/**
	 * Get list of versions of an API asset from Anypoint Exchange.
	 * @param accessToken Anypoint access token
	 * @param groupId Anypoint group id
	 * @param assetId Anypoint API asset id
	 * @return List of versions of API in Anypoint Exchange
	 */
	public List<AnypointExchangeAsset> getAPIVersionsFromAnypointExchange(String accessToken, String groupId, String assetId) {

		Map<String, List<Map<String, String>>> response = WebClient.builder().build()
				.get()
				.uri(String.format(ANYPOINT_API_VERSIONS_URL, groupId, assetId, groupId))
				.header(ANYPOINT_AUTHORIZATION_HEADER, String.format(ANYPOINT_AUTHORIZATION_BEARER, accessToken))
				.retrieve().bodyToMono(Map.class).block();

		return response.get(ANYPOINT_API_DEFINITIONS_PROPERTY).stream().map(d -> AnypointExchangeAsset.builder()
				.assetId(d.get(ANYPOINT_ASSET_ID_PROPERTY))
				.groupId(d.get(ANYPOINT_GROUP_ID_PROPERTY))
				.version(d.get(ANYPOINT_VERSION_PROPERTY))
				.productAPIVersion(d.get(ANYPOINT_PRODUCT_API_VERSION_PROPERTY))
				.name(d.get(ANYPOINT_NAME_PROPERTY))
				.type(d.get(ANYPOINT_TYPE_PROPERTY))
				.status(d.get(ANYPOINT_STATUS_PROPERTY))
				.build()
		).collect(Collectors.toList());
	}
}
