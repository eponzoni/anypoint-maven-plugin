package com.mulesoft.meetups;

import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MuleSoftPublishApiDocsMojo
 */
@Mojo(name = "publish-api-docs", defaultPhase = LifecyclePhase.VERIFY)
public class MuleSoftPublishApiDocsMojo extends AbstractMojo
{
    @Parameter(name = "username", required = true)
    private String username = null;

    @Parameter(name = "password", required = true)
    private String password = null;

    @Parameter(name = "apiName", required = true)
    private String apiName = null;

    @Parameter(name = "apiVersion", required = true)
    private String apiVersion = null;

    @Parameter(name = "documentationFilesLocation", required = true)
    private String documentationFilesLocation = null;

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

        //--- Get list of documentation files ---//
        List<File> files = Files.list(Paths.get(documentationFilesLocation)).map(Path::toFile).collect(Collectors.toList());

        //--- Prints a banner ---//
        this.printBanner();

        //--- Displays configuration ---//
        this.printConfigInfo(files);

        //--- Gets an Anypoint access token ---//
        AnypointToken accessToken = getAnypointAccessToken();

        //--- Gets details of current user based on access token ---//
        AnypointUser user = client.getUser(accessToken.getAccessToken());

        //--- Publish each page with contents from source files ---//
        files.stream().forEach(file -> {
            try {
                client.createAssetPage(
                        accessToken.getAccessToken(),
                        user.getOrganizationId(),
                        apiName,
                        file.getName().substring(0, file.getName().indexOf(".")),
                        apiVersion,
                        Files.readString(file.toPath()));
            } catch (Exception exception) {
                getLog().error(exception);
            }
        });
    }

    /**
     *
     * @return
     */
    private AnypointToken getAnypointAccessToken() {
        //---------------------------------------------------------//
        //--- GET A NEW ACCESS TOKEN BASED ON USERNAME/PASSWORD ---//
        //---------------------------------------------------------//
        return client.getToken(
                AnypointLogin.builder()
                        .username(username)
                        .password(password)
                        .build());
    }

    /**
     *
     * @param files
     * @throws IOException
     */
    private void printConfigInfo(List<File> files) throws IOException {
        getLog().info("");
        getLog().info("------------------------------------------------------------------------");
        getLog().info("                   PUBLISH API DOCUMENTATION - DETAILS                  ");
        getLog().info("------------------------------------------------------------------------");

        files.stream().forEach(file -> {
            getLog().info(String.format("Page.........: %s", file.getName().substring(0, file.getName().indexOf("."))));
        });

        getLog().info("------------------------------------------------------------------------");
        getLog().info("");
    }

    /**
     *
     * @param accessToken
     * @param user
     * @return
     */
    private AnypointExchangeClientApplication getOrCreateAnypointExchangeClientApplication(AnypointToken accessToken, AnypointUser user) {
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
        return clientApplication;
    }

    /**
     * Simply displays a cool-looking banner!
     */
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
                "$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\$$$$$$\\ \n" +
                "\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______\\______|\n" +
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
                "$$$$$$$\\          $$\\      $$\\$$\\         $$\\              $$$$$$\\ $$$$$$$\\$$$$$$\\       $$$$$$$\\                                                                 \n" +
                "$$  __$$\\         $$ |     $$ \\__|        $$ |            $$  __$$\\$$  __$$\\_$$  _|      $$  __$$\\                                                                \n" +
                "$$ |  $$ $$\\   $$\\$$$$$$$\\ $$ $$\\ $$$$$$$\\$$$$$$$\\        $$ /  $$ $$ |  $$ |$$ |        $$ |  $$ |$$$$$$\\  $$$$$$$\\ $$$$$$$\\                                     \n" +
                "$$$$$$$  $$ |  $$ $$  __$$\\$$ $$ $$  _____$$  __$$\\       $$$$$$$$ $$$$$$$  |$$ |        $$ |  $$ $$  __$$\\$$  _____$$  _____|                                    \n" +
                "$$  ____/$$ |  $$ $$ |  $$ $$ $$ \\$$$$$$\\ $$ |  $$ |      $$  __$$ $$  ____/ $$ |        $$ |  $$ $$ /  $$ $$ /     \\$$$$$$\\                                      \n" +
                "$$ |     $$ |  $$ $$ |  $$ $$ $$ |\\____$$\\$$ |  $$ |      $$ |  $$ $$ |      $$ |        $$ |  $$ $$ |  $$ $$ |      \\____$$\\                                     \n" +
                "$$ |     \\$$$$$$  $$$$$$$  $$ $$ $$$$$$$  $$ |  $$ |      $$ |  $$ $$ |    $$$$$$\\       $$$$$$$  \\$$$$$$  \\$$$$$$$\\$$$$$$$  |                                    \n" +
                "\\__|      \\______/\\_______/\\__\\__\\_______/\\__|  \\__|      \\__|  \\__\\__|    \\______|      \\_______/ \\______/ \\_______\\_______/                                     \n" +
                "                                                                                                                                                                  \n\n\n");
    }
}
