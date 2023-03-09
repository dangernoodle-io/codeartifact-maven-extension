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


public class CodeArtifactWagonTest
{
    private static final String URL = "codeartifact://domain-account.d.codeartifact.region.amazonaws.com/maven/repository";

    private AuthenticationInfo authInfo;

    private AwsCredentialsProvider provider;

    private Repository repository;

    private CodeArtifactWagon wagon;

    @BeforeEach
    public void beforeEach()
    {
        wagon = new CodeArtifactWagon();

        repository = new Repository("codeartifact", URL);
    }


    @Test
    public void testCredentialsProvider()
    {
        whenCreateCredentialsProvider();
        thenCredentialsProviderIsNull();

        givenStaticCredentials();
        whenCreateCredentialsProvider();
        thenCredentialsAreCorrect();
    }

    private void givenStaticCredentials()
    {
        authInfo = new AuthenticationInfo();
        authInfo.setUserName("AWS_ACCESS_KEY_ID");
        authInfo.setPassword("AWS_SECRET_ACCESS_KEY");
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

    private void whenConnect() throws ConnectionException, AuthenticationException
    {
        // don't care about the proxy
        wagon.connect(repository, authInfo, (ProxyInfoProvider) null);
    }

    private void whenCreateCredentialsProvider()
    {
        provider = wagon.createCredentialsProvider(authInfo);
    }
}
