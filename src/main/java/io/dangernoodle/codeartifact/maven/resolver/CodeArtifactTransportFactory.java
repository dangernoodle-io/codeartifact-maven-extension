package io.dangernoodle.codeartifact.maven.resolver;

import io.dangernoodle.codeartifact.maven.CodeArtifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Properties;


/**
 * Maven <code>TransporterFactory</code> implementation that handles authentication against <code>CodeArtifact</code>
 * prior to upload/download of artifacts.
 *
 * <p>This implementation is used by maven versions &gt;= 3.9.0</p>
 */
@Named("http")
public class CodeArtifactTransportFactory implements TransporterFactory
{
    private final CodeArtifact codeArtifact;

    private final TransporterFactory delegate;

    private final Provider<MavenSession> sessionProvider;

    @Inject
    public CodeArtifactTransportFactory(Provider<MavenSession> sessionProvider)
    {
        this.codeArtifact = createCodeArtifact();
        this.delegate = createTransporterFactory();
        this.sessionProvider = sessionProvider;
    }

    @Override
    public float getPriority()
    {
        return delegate.getPriority() * 2.0f;
    }

    @Override
    public Transporter newInstance(RepositorySystemSession session, RemoteRepository repository) throws NoTransporterException
    {
        if (codeArtifact.isCodeArtifactRepository(repository.getUrl()))
        {
            AuthenticationContext context = AuthenticationContext.forRepository(session, repository);
            CodeArtifact.Credentials credentials = createCredentials(repository.getHost(), context);

            repository = copyRepository(repository, credentials);
        }

        return delegate.newInstance(session, repository);
    }

    // visible for testing
    CodeArtifact createCodeArtifact()
    {
        return new CodeArtifact();
    }

    // visible for testing
    @SuppressWarnings("deprecation")
    TransporterFactory createTransporterFactory()
    {
        // keep an eye out for changes here - right now this constructor creates what is needed
        return new HttpTransporterFactory();
    }

    private String getAwsProfile()
    {
        final String profile = System.getProperty(CodeArtifact.AWS_PROFILE_PROPERTY_NAME);
        if (profile != null) {
            return profile;
        }
        final String profileFromEnv = System.getenv(CodeArtifact.AWS_PROFILE_ENV_VARIABLE_NAME);
        if (profileFromEnv != null) {
            return profileFromEnv;
        }
        return getAwsProfileFromProject();
    }

    private String getAwsProfileFromProject()
    {
        if (sessionProvider == null) {
            return null;
        }
        MavenSession session;
        try {
            session = sessionProvider.get();
        } catch (RuntimeException ex) {
            return null;
        }
        if (session == null) {
            return null;
        }
        MavenProject project = session.getCurrentProject();
        if (project == null) {
            return null;
        }
        Properties properties = project.getProperties();
        if (properties == null) {
            return null;
        }
        return properties.getProperty(CodeArtifact.AWS_PROFILE_PROJECT_PROPERTY_NAME);
    }

    private boolean areKeysSet(AuthenticationContext context)
    {
        return toUsername(context) != null && toPassword(context) != null;
    }

    private RemoteRepository copyRepository(RemoteRepository repository, CodeArtifact.Credentials credentials)
    {
        return new RemoteRepository.Builder(repository)
                .setAuthentication(new AuthenticationBuilder()
                        .addPassword(credentials.password)
                        .addUsername(credentials.username).build())
                .build();
    }

    private CodeArtifact.Credentials createCredentials(String host, AuthenticationContext context)
    {
        CodeArtifact.Credentials credentials = createCredentials(context);
        return codeArtifact.createCredentials(host, credentials);
    }

    private CodeArtifact.Credentials createCredentials(AuthenticationContext context)
    {
        if (context != null && areKeysSet(context)) 
        {
            return new CodeArtifact.Credentials(toUsername(context), toPassword(context));
        }
        final String awsProfile = getAwsProfile();
        if (awsProfile != null)
        {
            return new CodeArtifact.Credentials(awsProfile);
        }
        return CodeArtifact.Credentials.EMPTY;
    }

    private String toPassword(AuthenticationContext context)
    {
        return context.get(AuthenticationContext.PASSWORD);
    }

    private String toUsername(AuthenticationContext context)
    {
        return context.get(AuthenticationContext.USERNAME);
    }
}
