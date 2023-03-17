package io.dangernoodle.codeartifact.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.CodeartifactClientBuilder;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


@ExtendWith(MockitoExtension.class)
public class CodeArtifactTest
{
    public static final String PASSWORD = "AWS_SECRET_ACCESS_KEY";

    public static final String USERNAME = "AWS_ACCESS_KEY_ID";

    private static final String HOST = "domain-account.d.codeartifact.region.amazonaws.com";

    private CodeArtifact.Credentials credentials;

    private CodeArtifact delegate;

    private Instant expiration;

    private boolean isCodeArtifact;

    @Mock
    private CodeartifactClient mockClient;

    @Mock
    private CodeartifactClientBuilder mockClientBuilder;

    private CodeArtifact.Credentials mockCredentials;

    private GetAuthorizationTokenResponse mockResponse;

    @Captor
    private ArgumentCaptor<AwsCredentialsProvider> providerCaptor;

    @Captor
    private ArgumentCaptor<GetAuthorizationTokenRequest> requestCaptor;

    private String url;

    @BeforeEach
    public void beforeEach()
    {
        delegate = new CodeArtifact()
        {
            @Override
            @SuppressWarnings("resource")
            CodeartifactClient createClient(String region, Credentials credentials, CodeartifactClientBuilder builder)
            {
                super.createClient(region, credentials, builder);
                return mockClient;
            }
        };
    }

    @Test
    public void testCodeArtifactUrl()
    {
        givenACodeArtifactUrl();
        whenIsCodeArtifactUrl();
        thenUrlIsCodeArtifact();

        givenNotACodeArtifactUrl();
        whenIsCodeArtifactUrl();
        thenUrlIsNotCodeArtifact();
    }

    @Test
    public void testCreateCredentials()
    {
        givenDefaultCredentialsProvider();
        givenANonExpiredToken();
        givenATokenResponse();
        whenCreateCredentials();
        thenTokenRequestIsCorrect();
        thenCredentialsAreCreated();

        whenCreateCredentials();
        thenCachedCredentialsUsed();
        thenCredentialsAreCreated();
    }

    @Test
    public void testExpiredToken()
    {
        givenDefaultCredentialsProvider();
        givenAnExpiredToken();
        givenATokenResponse();
        whenCreateCredentials();
        thenTokenRequestIsCorrect();
        thenCredentialsAreCreated();

        whenCreateCredentials();
        thenCredentialsAreCreated();
    }

    @Test
    public void testOnlyKeySet()
    {
        givenAccessKeyOnly();
        whenCreateClient();
        thenRegionIsConfigured();
        thenDefaultProviderUsed();
    }

    @Test
    public void testOnlySecretSet()
    {
        givenSecretKeyOnly();
        whenCreateClient();
        thenRegionIsConfigured();
        thenDefaultProviderUsed();
    }

    @Test
    public void testStaticCredentials()
    {
        givenStaticCredentials();
        whenCreateClient();
        thenRegionIsConfigured();
        thenStaticProviderUsed();
    }

    private void givenACodeArtifactUrl()
    {
        url = "https://domain-account.d.codeartifact.region.amazonaws.com/maven/repository";
    }

    private void givenANonExpiredToken()
    {
        expiration = Instant.now().plus(1, ChronoUnit.HOURS);
    }

    private void givenATokenResponse()
    {
        mockResponse = GetAuthorizationTokenResponse.builder()
                                                    .authorizationToken("token")
                                                    .expiration(expiration)
                                                    .build();

        when(mockClient.getAuthorizationToken(any(GetAuthorizationTokenRequest.class))).thenReturn(mockResponse);
    }

    private void givenAccessKeyOnly()
    {
        mockCredentials = new CodeArtifact.Credentials(USERNAME, null);
    }

    private void givenAnExpiredToken()
    {
        expiration = Instant.now().minus(1, ChronoUnit.HOURS);
    }

    private void givenDefaultCredentialsProvider()
    {
        mockCredentials = new CodeArtifact.Credentials((String) null, null);
    }

    private void givenNotACodeArtifactUrl()
    {
        url = "https://domain.com";
    }

    private void givenSecretKeyOnly()
    {
        mockCredentials = new CodeArtifact.Credentials((String) null, PASSWORD);
    }

    private void givenStaticCredentials()
    {
        mockCredentials = new CodeArtifact.Credentials(USERNAME, PASSWORD);
    }

    private void thenCachedCredentialsUsed()
    {
        verifyNoMoreInteractions(mockClient);
    }

    private void thenCredentialsAreCreated()
    {
        assertNotNull(credentials);
        assertNotNull(credentials.expiration);
        assertEquals("codecommit", credentials.username);
        assertEquals("token", credentials.password);
    }

    private void thenDefaultProviderUsed()
    {
        verify(mockClientBuilder).credentialsProvider(null);
    }

    private void thenRegionIsConfigured()
    {
        verify(mockClientBuilder).region(Region.US_WEST_2);
    }

    private void thenStaticProviderUsed()
    {
        verify(mockClientBuilder).credentialsProvider(providerCaptor.capture());

        assertEquals(USERNAME, providerCaptor.getValue().resolveCredentials().accessKeyId());
        assertEquals(PASSWORD, providerCaptor.getValue().resolveCredentials().secretAccessKey());
    }

    private void thenTokenRequestIsCorrect()
    {
        verify(mockClient).getAuthorizationToken(requestCaptor.capture());
        verify(mockClient).close();

        assertEquals("domain", requestCaptor.getValue().domain());
        assertEquals("account", requestCaptor.getValue().domainOwner());
    }

    private void thenUrlIsCodeArtifact()
    {
        assertTrue(isCodeArtifact);
    }

    private void thenUrlIsNotCodeArtifact()
    {
        assertFalse(isCodeArtifact);
    }

    @SuppressWarnings("resource")
    private void whenCreateClient()
    {
        when(mockClientBuilder.credentialsProvider(any())).thenReturn(mockClientBuilder);
        when(mockClientBuilder.region(any())).thenReturn(mockClientBuilder);

        delegate.createClient("us-west-2", mockCredentials, mockClientBuilder);
    }

    private void whenCreateCredentials()
    {
        credentials = delegate.createCredentials(HOST, mockCredentials);
    }

    private void whenIsCodeArtifactUrl()
    {
        isCodeArtifact = delegate.isCodeArtifactRepository(url);
    }
}
