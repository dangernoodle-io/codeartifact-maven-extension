package io.dangernoodle.codeartifact.maven;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.CodeartifactClientBuilder;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Delegate class responsible for interfacing with <code>CodeArtifact</code>.
 */
public class CodeArtifact
{
    private static final String KEY = "codeartifact.maven.token.duration";

    private static final Pattern DOMAIN_AND_DOMAIN_OWNER_PATTERN = Pattern.compile("(.*)-(.*)");

    private CodeArtifact.Credentials cached;

    public CodeArtifact.Credentials createCredentials(String host, CodeArtifact.Credentials credentials)
    {
        if (cached == null || hasExpired(cached.expiration))
        {
            cached = generateCredentials(host, credentials);
        }

        return cached;
    }

    public boolean isCodeArtifactRepository(String url)
    {
        return url.contains(".d.codeartifact");
    }

    // visible for testing
    CodeartifactClient createClient(String region, Credentials credentials, CodeartifactClientBuilder builder)
    {
        return builder.credentialsProvider(createProvider(credentials))
                .region(Region.of(region))
                .build();
    }

    private AwsCredentialsProvider createProvider(Credentials credentials)
    {
        // null == use of the DefaultProviderChain
        return (credentials.username != null && credentials.password != null) ?
                createStaticCredentials(credentials) : null;
    }

    private AwsCredentialsProvider createStaticCredentials(Credentials credentials)
    {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(credentials.username, credentials.password));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private GetAuthorizationTokenRequest createTokenRequest(String domainOwner)
    {
        Matcher matcher = DOMAIN_AND_DOMAIN_OWNER_PATTERN.matcher(domainOwner);
        matcher.matches();

        return GetAuthorizationTokenRequest.builder()
                .domain(matcher.group(1))
                .domainOwner(matcher.group(2))
                .durationSeconds(getTokenDuration())
                .build();
    }

    private Credentials generateCredentials(String host, Credentials credentials)
    {
        // <domain>-<owner>.d.codeartifact.us-west-2.amazonaws.com
        String[] parts = host.split("\\.");

        try (CodeartifactClient client = createClient(parts[3], credentials, CodeartifactClient.builder()))
        {
            GetAuthorizationTokenRequest request = createTokenRequest(parts[0]);
            GetAuthorizationTokenResponse response = client.getAuthorizationToken(request);

            return new CodeArtifact.Credentials(response.authorizationToken(), response.expiration());
        }
    }

    private long getTokenDuration()
    {
        return Duration.ofMinutes(Long.parseLong(System.getProperty(KEY, "15")))
                .getSeconds();
    }

    private boolean hasExpired(Instant instant)
    {
        return Optional.ofNullable(instant)
                .filter(Instant.now()::isAfter)
                .map(i -> true)
                .isPresent();
    }

    public static class Credentials
    {
        public static final Credentials EMPTY = new Credentials(null, (String) null);

        public final Instant expiration;

        public final String password;

        public final String username;

        private Credentials(String password, Instant timestamp)
        {
            this.username = "codecommit";
            this.password = password;
            this.expiration = timestamp;
        }

        public Credentials(String username, String password)
        {
            this.username = username;
            this.password = password;
            this.expiration = null;
        }
    }
}
