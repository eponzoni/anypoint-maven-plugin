package com.mulesoft.meetups;

import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MuleSoftSmokeTestMojo
 */
@Mojo(name = "smoke-test", defaultPhase = LifecyclePhase.VERIFY)
public class MuleSoftSmokeTestMojo extends AbstractMojo
{
    @Parameter(name = "username", required = true)
    private String username = null;

    @Parameter(name = "password", required = true)
    private String password = null;

    @Parameter(name = "apiName", required = true)
    private String apiName = null;

    @Parameter(name = "environmentName", required = true)
    private String environmentName = null;

    @Parameter(name = "endpointUrl", required = true)
    private String endpointUrl = null;

    /**
     * Anypoint REST API client.
     */
    private final AnypointRestAPIClient client = new AnypointRestAPIClient();

    /**
     * Execute smoke test.
     *
     * @throws MojoExecutionException
     */
    @SneakyThrows
    public void execute() throws MojoExecutionException {

        this.printBanner();
        getLog().info("");
        getLog().info("------------------------------------------------------------------------");
        getLog().info("                     SMOKE TEST - DETAILS                               ");
        getLog().info("------------------------------------------------------------------------");
        getLog().info(String.format("API.........: %s", apiName));
        getLog().info(String.format("Environment.:  %s", environmentName));
        getLog().info(String.format("Endpoint URL: %s", endpointUrl));
        getLog().info("------------------------------------------------------------------------");
        getLog().info("");
        //---------------------------------------------------------//
        //--- GET A NEW ACCESS TOKEN BASED ON USERNAME/PASSWORD ---//
        //---------------------------------------------------------//
        AnypointToken accessToken = client.getToken(
                AnypointLogin.builder()
                        .username(username)
                        .password(password)
                        .build());

        //-------------------------------------------------------------//
        //--- GET DETAILS OF THE CURRENT USER BASED ON ACCESS TOKEN ---//
        //-------------------------------------------------------------//
        AnypointUser user = client.getUser(accessToken.getAccessToken());

        //--------------------------------------------------//
        //--- CHECK FOR EXISTING APPLICATION IN EXCHANGE ---//
        //--------------------------------------------------//
        List<AnypointExchangeClientApplication> clientApplications = client.getClientApplicationsInExchange(accessToken.getAccessToken(), user.getOrganizationId());
        Optional<AnypointExchangeClientApplication> optionalClientApplication = clientApplications.stream().filter(f -> f.getDescription().equalsIgnoreCase("Temp Application")).findFirst();
        AnypointExchangeClientApplication clientApplication = null;

        //-------------------------------------//
        //--- IF APPLICATION DOES NOT EXIST ---//
        //-------------------------------------//
        if (optionalClientApplication.isPresent() == false) {

            //---------------------------------------------------//
            //--- CREATE A NEW CLIENT APPLICATION IN EXCHANGE ---//
            //---------------------------------------------------//
            clientApplication = AnypointExchangeClientApplication.builder()
                    .name("Temp Application")
                    .description("Temp Application")
                    .url("http://localhost")
                    .build();

            clientApplication.getGrantTypes().add("client_credentials");
            client.createAPIClientApplication(accessToken.getAccessToken(), user.getOrganizationId(), clientApplication);
        } else {
            clientApplication = optionalClientApplication.get();
            clientApplication.getId();
        }

        //----------------------------------------//
        //--- GET SPECIFIC ENVIRONMENT DETAILS ---//
        //----------------------------------------//
        List<AnypointEnvironment> environments = client.getEnvironments(accessToken.getAccessToken(),  user.getOrganizationId());
        Optional<AnypointEnvironment> environment = environments.stream().filter(e -> e.getName().equalsIgnoreCase(environmentName)).findFirst();

        if (environment.isPresent() == false) {

            getLog().error(String.format("Environment: %s not found.", environmentName));
            return;
        }

        //-----------------------------------------------------------//
        //--- GET DETAILS OF API INSTANCE WITHIN THAT ENVIRONMENT ---//
        //-----------------------------------------------------------//
        List<AnypointAPI> environmentAPIs = client.getAPIsByEnvironmentId(
                accessToken.getAccessToken(),
                user.getOrganizationId(),
                environment.get().getId());

        Optional<AnypointAPI> api = environmentAPIs.stream().filter(a -> a.getAssetId().equalsIgnoreCase(apiName)).findFirst();

        if (api.isPresent() == false) {
            getLog().error(String.format("API: %s in Environment: %s not found.", apiName, environment));
            return;
        }

        //--------------------------------------------------------------//
        //--- CREATE A NEW SLA TIER FOR THAT API IN THAT ENVIRONMENT ---//
        //--------------------------------------------------------------//
        AnypointAPISlaTier slaTier = AnypointAPISlaTier.builder()
                .autoApprove(true)
                .status("ACTIVE")
                .description("Automated Testing SLA Tier")
                .name("Automated Testing SLA Tier")
                .limits(new ArrayList<>())
                .apiVersionId(api.get().getId())
                .build();

        slaTier.getLimits().add(
                AnypointAPISlaTierLimit.builder()
                        .visible(true)
                        .maximumRequests(1000)
                        .timePeriodInMilliseconds(1000)
                        .build());

        Long slaTierId = client.createAPISlaTier(accessToken.getAccessToken(),
                user.getOrganizationId(),
                environment.get().getId(),
                api.get().getId(),
                slaTier);

        //----------------------------------------------------------------------------------------------------//
        //--- CREATE A NEW CONTRACT: API IN ENVIRONMENT + CLIENT APPLICATION IN AUTOMATED TESTING SLA TIER ---//
        //----------------------------------------------------------------------------------------------------//
        AnypointAPIContract contract = AnypointAPIContract.builder()
                .apiId(String.valueOf(api.get().getId()))
                .environmentId(environment.get().getId())
                .instanceType("api")
                .requestedTierId(slaTierId)
                .acceptedTerms(true)
                .organizationId(user.getOrganizationId())
                .groupId(user.getOrganizationId())
                .assetId(apiName)
                .version(api.get().getAssetVersion())
                .versionGroup(api.get().getAssetVersion())
                .build();

        Long apiClientContractId = client.createAPIClientContract(
                                    accessToken.getAccessToken(),
                                    user.getOrganizationId(),
                                    clientApplication.getId(),
                                    contract);

        WebClient.builder().build()
            .get()
                .uri(endpointUrl)
                .header("X-Client-ID", clientApplication.getClientId())
                .header("X-Client-Secret", clientApplication.getClientSecret())
                .exchange()
                .doOnSuccess(clientResponse -> {
                    getLog().info("");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info("                     SMOKE TEST - RESULTS                               ");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info(String.format("HTTP method.: %s", "GET"));
                    getLog().info(String.format("Endpoint URL: %s", endpointUrl));
                    getLog().info(String.format("HTTP status.: %s - %s", HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase()));
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info( "Result......: SUCCESS");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info("");
                })
                .doOnError(clientResponse -> {
                    getLog().info("");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info("                     SMOKE TEST - RESULTS                               ");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info(String.format("HTTP method..: %s", "GET"));
                    getLog().info(String.format("Endpoint URL.: %s", endpointUrl));
                    getLog().info(String.format("Error Message: %s", clientResponse.getMessage()));
                    getLog().info(  "Result.......: FAILURE");
                    getLog().info("------------------------------------------------------------------------");
                    getLog().info("");
                }).block();

        client.deleteAPIClientContract(
                accessToken.getAccessToken(),
                user.getOrganizationId(),

                environment.get().getId(),
                api.get().getId(),
                apiClientContractId);

        client.deleteAPISlaTier(
                accessToken.getAccessToken(),
                user.getOrganizationId(),
                environment.get().getId(),
                api.get().getId(),
                slaTierId);
    }

    private void printBanner() {
        getLog().info("\n\n\n$$\\      $$\\         $$\\          $$$$$$\\           $$$$$$\\   $$\\           $$\\      $$\\                   $$\\                                                    \n" +
                "$$$\\    $$$ |        $$ |        $$  __$$\\         $$  __$$\\  $$ |          $$$\\    $$$ |                  $$ |                                                   \n" +
                "$$$$\\  $$$$ $$\\   $$\\$$ |$$$$$$\\ $$ /  \\__|$$$$$$\\ $$ /  \\__$$$$$$\\         $$$$\\  $$$$ |$$$$$$\\  $$$$$$\\$$$$$$\\  $$\\   $$\\ $$$$$$\\                               \n" +
                "$$\\$$\\$$ $$ $$ |  $$ $$ $$  __$$\\\\$$$$$$\\ $$  __$$\\$$$$\\    \\_$$  _|        $$\\$$\\$$ $$ $$  __$$\\$$  __$$\\_$$  _| $$ |  $$ $$  __$$\\                              \n" +
                "$$ \\$$$  $$ $$ |  $$ $$ $$$$$$$$ |\\____$$\\$$ /  $$ $$  _|     $$ |          $$ \\$$$  $$ $$$$$$$$ $$$$$$$$ |$$ |   $$ |  $$ $$ /  $$ |                             \n" +
                "$$ |\\$  /$$ $$ |  $$ $$ $$   ____$$\\   $$ $$ |  $$ $$ |       $$ |$$\\       $$ |\\$  /$$ $$   ____$$   ____|$$ |$$\\$$ |  $$ $$ |  $$ |                             \n" +
                "$$ | \\_/ $$ \\$$$$$$  $$ \\$$$$$$$\\\\$$$$$$  \\$$$$$$  $$ |       \\$$$$  |      $$ | \\_/ $$ \\$$$$$$$\\\\$$$$$$$\\ \\$$$$  \\$$$$$$  $$$$$$$  |                             \n" +
                "\\__|     \\__|\\______/\\__|\\_______|\\______/ \\______/\\__|        \\____/       \\__|     \\__|\\_______|\\_______| \\____/ \\______/$$  ____/                              \n" +
                "                                                                                                                           $$ |                                   \n" +
                "                                                                                                                           $$ |                                   \n" +
                "                                                                                                                           \\__|                                   \n" +
                " $$$$$$\\                   $$\\      $$\\                        $$\\                     $$$$$$$\\                         $$\\ $$$$$$\\  $$$$$$\\  $$$$$$\\   $$\\       \n" +
                "$$  __$$\\                  $$ |     $$ |                       $$ |                    $$  __$$\\                       $$  $$  __$$\\$$$ __$$\\$$  __$$\\$$$$ |      \n" +
                "$$ /  $$ $$\\   $$\\ $$$$$$$\\$$ |  $$\\$$ |$$$$$$\\ $$$$$$$\\  $$$$$$$ |                    $$ |  $$ |$$$$$$\\  $$$$$$$\\    $$  /\\__/  $$ $$$$\\ $$ \\__/  $$ \\_$$ |      \n" +
                "$$$$$$$$ $$ |  $$ $$  _____$$ | $$  $$ |\\____$$\\$$  __$$\\$$  __$$ |      $$$$$$\\       $$ |  $$ $$  __$$\\$$  _____|  $$  /  $$$$$$  $$\\$$\\$$ |$$$$$$  | $$ |      \n" +
                "$$  __$$ $$ |  $$ $$ /     $$$$$$  /$$ |$$$$$$$ $$ |  $$ $$ /  $$ |      \\______|      $$ |  $$ $$$$$$$$ $$ /       $$  /  $$  ____/$$ \\$$$$ $$  ____/  $$ |      \n" +
                "$$ |  $$ $$ |  $$ $$ |     $$  _$$< $$ $$  __$$ $$ |  $$ $$ |  $$ |                    $$ |  $$ $$   ____$$ |      $$  /   $$ |     $$ |\\$$$ $$ |       $$ |      \n" +
                "$$ |  $$ \\$$$$$$  \\$$$$$$$\\$$ | \\$$\\$$ \\$$$$$$$ $$ |  $$ \\$$$$$$$ |                    $$$$$$$  \\$$$$$$$\\\\$$$$$$$\\$$  /    $$$$$$$$\\\\$$$$$$  $$$$$$$$\\$$$$$$\\     \n" +
                "\\__|  \\__|\\______/ \\_______\\__|  \\__\\__|\\_______\\__|  \\__|\\_______|                    \\_______/ \\_______|\\_______\\__/     \\________|\\______/\\________\\______|    \n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\ \n" +
                "\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______|\n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "                                                                                                                                                                  \n" +
                "$$$$$$$\\                                       $$\\      $$\\                                           $$$$$$$\\ $$\\                  $$\\                           \n" +
                "$$  __$$\\                                      $$$\\    $$$ |                                          $$  __$$\\$$ |                 \\__|                          \n" +
                "$$ |  $$ |$$$$$$\\ $$$$$$\\$$$$\\  $$$$$$\\        $$$$\\  $$$$ |$$$$$$\\$$\\    $$\\ $$$$$$\\ $$$$$$$\\        $$ |  $$ $$ $$\\   $$\\ $$$$$$\\ $$\\$$$$$$$\\ $$\\               \n" +
                "$$ |  $$ $$  __$$\\$$  _$$  _$$\\$$  __$$\\       $$\\$$\\$$ $$ |\\____$$\\$$\\  $$  $$  __$$\\$$  __$$\\       $$$$$$$  $$ $$ |  $$ $$  __$$\\$$ $$  __$$\\\\__|              \n" +
                "$$ |  $$ $$$$$$$$ $$ / $$ / $$ $$ /  $$ |      $$ \\$$$  $$ |$$$$$$$ \\$$\\$$  /$$$$$$$$ $$ |  $$ |      $$  ____/$$ $$ |  $$ $$ /  $$ $$ $$ |  $$ |                 \n" +
                "$$ |  $$ $$   ____$$ | $$ | $$ $$ |  $$ |      $$ |\\$  /$$ $$  __$$ |\\$$$  / $$   ____$$ |  $$ |      $$ |     $$ $$ |  $$ $$ |  $$ $$ $$ |  $$ $$\\               \n" +
                "$$$$$$$  \\$$$$$$$\\$$ | $$ | $$ \\$$$$$$  |      $$ | \\_/ $$ \\$$$$$$$ | \\$  /  \\$$$$$$$\\$$ |  $$ |      $$ |     $$ \\$$$$$$  \\$$$$$$$ $$ $$ |  $$ \\__|              \n" +
                "\\_______/ \\_______\\__| \\__| \\__|\\______/       \\__|     \\__|\\_______|  \\_/    \\_______\\__|  \\__|      \\__|     \\__|\\______/ \\____$$ \\__\\__|  \\__|                 \n" +
                "                                                                                                                           $$\\   $$ |                             \n" +
                "                                                                                                                           \\$$$$$$  |                             \n" +
                "                                                                                                                            \\______/                              \n" +
                "$$\\      $$\\         $$\\          $$$$$$\\           $$$$$$\\   $$\\            $$$$$$\\                   $$\\$$\\                  $$\\    $$\\                         \n" +
                "$$$\\    $$$ |        $$ |        $$  __$$\\         $$  __$$\\  $$ |          $$  __$$\\                  $$ \\__|                 $$ |   \\__|                        \n" +
                "$$$$\\  $$$$ $$\\   $$\\$$ |$$$$$$\\ $$ /  \\__|$$$$$$\\ $$ /  \\__$$$$$$\\         $$ /  $$ |$$$$$$\\  $$$$$$\\ $$ $$\\ $$$$$$$\\$$$$$$\\$$$$$$\\  $$\\ $$$$$$\\ $$$$$$$\\        \n" +
                "$$\\$$\\$$ $$ $$ |  $$ $$ $$  __$$\\\\$$$$$$\\ $$  __$$\\$$$$\\    \\_$$  _|        $$$$$$$$ $$  __$$\\$$  __$$\\$$ $$ $$  _____\\____$$\\_$$  _| $$ $$  __$$\\$$  __$$\\       \n" +
                "$$ \\$$$  $$ $$ |  $$ $$ $$$$$$$$ |\\____$$\\$$ /  $$ $$  _|     $$ |          $$  __$$ $$ /  $$ $$ /  $$ $$ $$ $$ /     $$$$$$$ |$$ |   $$ $$ /  $$ $$ |  $$ |      \n" +
                "$$ |\\$  /$$ $$ |  $$ $$ $$   ____$$\\   $$ $$ |  $$ $$ |       $$ |$$\\       $$ |  $$ $$ |  $$ $$ |  $$ $$ $$ $$ |    $$  __$$ |$$ |$$\\$$ $$ |  $$ $$ |  $$ |      \n" +
                "$$ | \\_/ $$ \\$$$$$$  $$ \\$$$$$$$\\\\$$$$$$  \\$$$$$$  $$ |       \\$$$$  |      $$ |  $$ $$$$$$$  $$$$$$$  $$ $$ \\$$$$$$$\\$$$$$$$ |\\$$$$  $$ \\$$$$$$  $$ |  $$ |      \n" +
                "\\__|     \\__|\\______/\\__|\\_______|\\______/ \\______/\\__|        \\____/       \\__|  \\__$$  ____/$$  ____/\\__\\__|\\_______\\_______| \\____/\\__|\\______/\\__|  \\__|      \n" +
                "                                                                                     $$ |     $$ |                                                                \n" +
                "                                                                                     $$ |     $$ |                                                                \n" +
                "                                                                                     \\__|     \\__|                                                                \n" +
                " $$$$$$\\                       $$\\                      $$$$$$$$\\                 $$\\                                                                             \n" +
                "$$  __$$\\                      $$ |                     \\__$$  __|                $$ |                                                                            \n" +
                "$$ /  \\__$$$$$$\\$$$$\\  $$$$$$\\ $$ |  $$\\ $$$$$$\\           $$ |$$$$$$\\  $$$$$$$\\$$$$$$\\                                                                           \n" +
                "\\$$$$$$\\ $$  _$$  _$$\\$$  __$$\\$$ | $$  $$  __$$\\          $$ $$  __$$\\$$  _____\\_$$  _|                                                                          \n" +
                " \\____$$\\$$ / $$ / $$ $$ /  $$ $$$$$$  /$$$$$$$$ |         $$ $$$$$$$$ \\$$$$$$\\   $$ |                                                                            \n" +
                "$$\\   $$ $$ | $$ | $$ $$ |  $$ $$  _$$< $$   ____|         $$ $$   ____|\\____$$\\  $$ |$$\\                                                                         \n" +
                "\\$$$$$$  $$ | $$ | $$ \\$$$$$$  $$ | \\$$\\\\$$$$$$$\\          $$ \\$$$$$$$\\$$$$$$$  | \\$$$$  |                                                                        \n" +
                " \\______/\\__| \\__| \\__|\\______/\\__|  \\__|\\_______|         \\__|\\_______\\_______/   \\____/                                                                         \n" +
                "                                                                                                                                                                  \n\n\n");
    }
}
