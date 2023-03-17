package io.dangernoodle.codeartifact.maven.wagon;

import static io.dangernoodle.codeartifact.maven.CodeArtifactTest.PASSWORD;
import static io.dangernoodle.codeartifact.maven.CodeArtifactTest.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dangernoodle.codeartifact.maven.CodeArtifact;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CodeArtifactWagonTest
{
    @Captor
    private ArgumentCaptor<CodeArtifact.Credentials> credsCaptor;

    private AuthenticationInfo mockAuthInfo;

    @Mock
    private CodeArtifact mockCodeArtifact;

    @Mock
    private CodeArtifact.Credentials mockCredentials;

    @Mock
    private Repository mockRepository;

    private CodeArtifactWagon wagon;

    @BeforeEach
    public void beforeEach()
    {
        wagon = new CodeArtifactWagon()
        {
            @Override
            CodeArtifact createCodeArtifact()
            {
                return mockCodeArtifact;
            }
        };

        when(mockRepository.getHost()).thenReturn("doesnt-matter");
        when(mockRepository.getUrl()).thenReturn("https://doesnt-matter.cant.be.null.com");
    }

    @Test
    public void testNoCredentials() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        whenConnectToRepository();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testNotRepository() throws Exception
    {
        whenConnectToRepository();
        thenCredentialsAreNotGenerated();
    }

    @Test
    public void testOnlyKeySet() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAccessKeyOnly();
        whenConnectToRepository();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testOnlySecretSet() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAccessSecretOnly();
        whenConnectToRepository();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testStaticCredentials() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenStaticCredentials();
        whenConnectToRepository();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreStatic();
    }

    private void givenAUrlThatIsCodeArtifact()
    {
        when(mockCodeArtifact.isCodeArtifactRepository(anyString())).thenReturn(true);
        when(mockCodeArtifact.createCredentials(anyString(), any())).thenReturn(mockCredentials);
    }

    private void givenAccessKeyOnly()
    {
        mockAuthInfo = new AuthenticationInfo();
        mockAuthInfo.setUserName(USERNAME);
    }

    private void givenAccessSecretOnly()
    {
        mockAuthInfo = new AuthenticationInfo();
        mockAuthInfo.setPassword(PASSWORD);
    }

    private void givenStaticCredentials()
    {
        mockAuthInfo = new AuthenticationInfo();
        mockAuthInfo.setUserName(USERNAME);
        mockAuthInfo.setPassword(PASSWORD);
    }

    private void thenCredentialsAreGenerated()
    {
        verify(mockCodeArtifact).createCredentials(anyString(), credsCaptor.capture());
    }

    private void thenCredentialsAreNotGenerated()
    {
        verify(mockCodeArtifact, times(0)).createCredentials(anyString(), any());
    }

    private void thenPassedCredentialsAreNull()
    {
        assertNull(credsCaptor.getValue().username);
        assertNull(credsCaptor.getValue().password);
    }

    private void thenPassedCredentialsAreStatic()
    {
        assertEquals(USERNAME, credsCaptor.getValue().username);
        assertEquals(PASSWORD, credsCaptor.getValue().password);
    }

    private void whenConnectToRepository() throws ConnectionException, AuthenticationException
    {
        // don't care about the proxy
        wagon.connect(mockRepository, mockAuthInfo, (ProxyInfoProvider) null);
    }
}
