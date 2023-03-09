package io.dangernoodle.maven.wagon.codeartifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;


public class CodeArtifactWagonTest
{
    private static final String URL = "codeartifact://domain-account.d.codeartifact.region.amazonaws.com/maven/repository";

    private AuthenticationInfo authInfo;

    private int count;

    private String domainOwner;

    private AwsCredentialsProvider provider;

    private Repository repository;

    private GetAuthorizationTokenRequest request;

    private CodeArtifactWagon wagon;

    @BeforeEach
    public void beforeEach()
    {
        repository = new Repository("codeartifact", URL);

        wagon = new CodeArtifactWagon()
        {
            @Override
            String getAuthorizationToken(CodeartifactClient client, String[] host)
            {
                // mockito wasn't working w/ the client interface, so doing this instead
                count++;
                return "token";
            }
        };
    }

    @Test
    public void testConnect() throws Exception
    {
        whenConnect();
        thenAuthInfoIsChanged();

        whenConnect();
        thenAuthInfoUpdateSkipped();
        thenAuthInfoIsChanged();
    }

    @Test
    public void testCreateRequest()
    {
        givenADomainOwner();
        whenCreateRequest();
        thenRequestIsCorrect();

    }

    @Test
    public void testCredentialsProviders()
    {
        whenCreateCredentialsProvider();
        thenCredentialsProviderIsNull();

        givenAccessKeyOnly();
        whenCreateCredentialsProvider();
        thenCredentialsProviderIsNull();

        givenAccessSecretOnly();
        whenCreateCredentialsProvider();
        thenCredentialsProviderIsNull();

        givenStaticCredentials();
        whenCreateCredentialsProvider();
        thenCredentialsAreCorrect();
    }

    private void givenADomainOwner()
    {
        domainOwner = "domain-owner";
    }

    private void givenAccessKeyOnly()
    {
        authInfo = new AuthenticationInfo();
        authInfo.setUserName("AWS_ACCESS_KEY_ID");
    }

    private void givenAccessSecretOnly()
    {
        authInfo = new AuthenticationInfo();
        authInfo.setPassword("AWS_SECRET_ACCESS_KEY");
    }

    private void givenStaticCredentials()
    {
        authInfo = new AuthenticationInfo();
        authInfo.setUserName("AWS_ACCESS_KEY_ID");
        authInfo.setPassword("AWS_SECRET_ACCESS_KEY");
    }

    private void thenAuthInfoIsChanged()
    {
        assertEquals("aws", wagon.awsAuthInfo.getUserName());
    }

    private void thenAuthInfoUpdateSkipped()
    {
        // mockito isn't, so we can verify this way
        assertEquals(1, count);
    }

    private void thenCredentialsAreCorrect()
    {
        assertNotNull(provider);
        assertEquals(authInfo.getUserName(), provider.resolveCredentials().accessKeyId());
        assertEquals(authInfo.getPassword(), provider.resolveCredentials().secretAccessKey());
    }

    private void thenCredentialsProviderIsNull()
    {
        assertNull(provider);
    }

    private void thenRequestIsCorrect()
    {
        assertEquals("domain", request.domain());
        assertEquals("owner", request.domainOwner());
    }

    private void whenConnect() throws ConnectionException, AuthenticationException
    {
        // don't care about the proxy
        wagon.connect(repository, authInfo, (ProxyInfoProvider) null);
    }

    private void whenCreateCredentialsProvider()
    {
        provider = wagon.createCredentialsProvider(authInfo);
    }

    private void whenCreateRequest()
    {
        request = wagon.createRequest(domainOwner);
    }
}
