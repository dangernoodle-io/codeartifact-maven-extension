package io.dangernoodle.maven.wagon.codeartifact;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;

import java.util.Optional;


/**
 * Maven <code>http</code> wagon implementation that handles authentication against <code>CodeArtifact</code>
 * prior to upload/download of artifacts.
 */
public class CodeArtifactWagon extends AbstractHttpClientWagon
{
    // visble for testing
    AuthenticationInfo awsAuthInfo;

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException
    {
        if (awsAuthInfo == null)
        {
            // <domain>-<owner>.d.codeartifact.us-west-2.amazonaws.com/<repo-type>/<repo-name>
            String[] host = repository.getHost().split("\\.");
            awsAuthInfo = createCodeArtifactAuthentication(host, authenticationInfo);
        }

        super.connect(replaceProtocol(repository), awsAuthInfo, proxyInfoProvider);
    }

    // visible for testing
    CodeartifactClient createCodeArtifactClient(String region, AuthenticationInfo auth)
    {
        return CodeartifactClient.builder()
                                 .credentialsProvider(createCredentialsProvider(auth))
                                 .region(Region.of(region))
                                 .build();
    }

    // visible for testing
    AwsCredentialsProvider createCredentialsProvider(AuthenticationInfo authenticationInfo)
    {
        return Optional.ofNullable(authenticationInfo)
                       .filter(auth -> !(auth.getUserName() == null || auth.getPassword() == null))
                       .map(this::createStaticCredentialsProvider)
                       .orElse(null);
    }

    // visble for testing
    GetAuthorizationTokenRequest createRequest(String domainOwner)
    {
        String[] parts = domainOwner.split("-");
        return GetAuthorizationTokenRequest.builder()
                                           .domain(parts[0])
                                           .domainOwner(parts[1])
                                           .build();
    }

    // visble for testing
    String getAuthorizationToken(CodeartifactClient client, String[] host)
    {
        return client.getAuthorizationToken(createRequest(host[0]))
                     .authorizationToken();
    }

    private AuthenticationInfo createCodeArtifactAuthentication(String[] host, AuthenticationInfo auth)
    {
        try (CodeartifactClient client = createCodeArtifactClient(host[3], auth))
        {
            String token = getAuthorizationToken(client, host);

            AuthenticationInfo codeArtifact = new AuthenticationInfo();
            codeArtifact.setUserName("aws");
            codeArtifact.setPassword(token);

            return codeArtifact;
        }
    }

    private AwsCredentialsProvider createStaticCredentialsProvider(AuthenticationInfo auth)
    {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(auth.getUserName(), auth.getPassword()));
    }

    private Repository replaceProtocol(Repository repository)
    {
        String url = repository.getUrl().replaceFirst("codeartifact", "https");
        return new Repository(repository.getId(), url);
    }
}