package io.dangernoodle.codeartifact.maven.resolver;

import static io.dangernoodle.codeartifact.maven.CodeArtifactTest.AWS_PROFILE;
import static io.dangernoodle.codeartifact.maven.CodeArtifactTest.PASSWORD;
import static io.dangernoodle.codeartifact.maven.CodeArtifactTest.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dangernoodle.codeartifact.maven.CodeArtifact;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
public class CodeArtifactTransportFactoryTest
{
    @Captor
    private ArgumentCaptor<CodeArtifact.Credentials> credsCaptor;

    private CodeArtifactTransportFactory factory;

    private Authentication mockAuthentication;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Mock
    private CodeArtifact mockCodeArtifact;

    @Mock
    private CodeArtifact.Credentials mockCredentials;

    @Mock
    private TransporterFactory mockFactory;

    @Mock
    private RepositorySystemSession mockSession;

    @BeforeEach
    public void beforeEach()
    {
        factory = new CodeArtifactTransportFactory()
        {
            @Override
            CodeArtifact createCodeArtifact()
            {
                return mockCodeArtifact;
            }

            @Override
            TransporterFactory createTransporterFactory()
            {
                return mockFactory;
            }
        };

        System.clearProperty(CodeArtifact.AWS_PROFILE_PROPERTY_NAME);
    }

    @Test
    public void testNoCredentials() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testNotRepository() throws Exception
    {
        givenAUrlThatIsntCodeArtifact();
        whenCreateTransportFactory();
        thenCredentialsAreNotGenerated();
    }

    @Test
    public void testOnlyKeySet() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAccessKeyOnly();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testOnlySecretSet() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAccessSecretOnly();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreNull();
    }

    @Test
    public void testPriority()
    {
        givenADelegatePriority();
        thenPriorityIsGreater();
    }

    @Test
    public void testStaticCredentials() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenStaticCredentials();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreStatic();
    }

    @Test
    public void testProfileCredentialsFromEnvVariable() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAProfileEnvVariable();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsContainProfile();
    }

    @Test
    public void testProfileCredentialsFromProperty() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenAProfileProperty();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsContainProfile();
    }

    @Test
    public void testStaticCredentialsTakePresentsOverProfileCredentials() throws Exception
    {
        givenAUrlThatIsCodeArtifact();
        givenStaticCredentials();
        givenAProfileProperty();
        whenCreateTransportFactory();
        thenCredentialsAreGenerated();
        thenPassedCredentialsAreStatic();
        thenPassedCredentialsDoNotContainProfile();
    }

    private RemoteRepository createMockRepository()
    {
        // url doesn't matter b/c of mocking
        return new RemoteRepository.Builder("id", null, "http://domain.com")
                .setAuthentication(mockAuthentication)
                .build();
    }

    private void givenADelegatePriority()
    {
        when(mockFactory.getPriority()).thenReturn(1.0f);
    }

    private void givenAUrlThatIsCodeArtifact()
    {
        when(mockCodeArtifact.isCodeArtifactRepository(anyString())).thenReturn(true);
        when(mockCodeArtifact.createCredentials(anyString(), any())).thenReturn(mockCredentials);
    }

    private void givenAUrlThatIsntCodeArtifact()
    {
        when(mockCodeArtifact.isCodeArtifactRepository(anyString())).thenReturn(false);
    }

    private void givenAccessKeyOnly()
    {
        mockAuthentication = new AuthenticationBuilder().addUsername(USERNAME).build();
    }

    private void givenAccessSecretOnly()
    {
        mockAuthentication = new AuthenticationBuilder().addPassword(PASSWORD).build();
    }

    private void givenStaticCredentials()
    {
        mockAuthentication = new AuthenticationBuilder().addUsername(USERNAME)
                                                        .addPassword(PASSWORD)
                                                        .build();
    }

    private void givenAProfileEnvVariable()
    {
        environmentVariables.set(CodeArtifact.AWS_PROFILE_ENV_VARIABLE_NAME, AWS_PROFILE);
    }

    private void givenAProfileProperty()
    {
        System.setProperty(CodeArtifact.AWS_PROFILE_PROPERTY_NAME, AWS_PROFILE);
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

    private void thenPassedCredentialsContainProfile()
    {
        assertEquals(AWS_PROFILE, credsCaptor.getValue().awsProfile);
    }

    private void thenPassedCredentialsDoNotContainProfile()
    {
        assertNull(credsCaptor.getValue().awsProfile);
    }

    private void thenPriorityIsGreater()
    {
        assertTrue(mockFactory.getPriority() < factory.getPriority());
    }

    private void whenCreateTransportFactory() throws NoTransporterException
    {
        RemoteRepository mockRepository = createMockRepository();
        factory.newInstance(mockSession, mockRepository);
    }
}
