package io.dangernoodle.codeartifact.maven.resolver;

import io.dangernoodle.codeartifact.maven.CodeArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.inject.Named;
import java.util.Optional;


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

    public CodeArtifactTransportFactory()
    {
        this.codeArtifact = createCodeArtifact();
        this.delegate = createTransporterFactory();
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
        return Optional.ofNullable(context)
                       .filter(this::areKeysSet)
                       .map(this::toCredentials)
                       .orElse(CodeArtifact.Credentials.EMPTY);
    }

    private CodeArtifact.Credentials toCredentials(AuthenticationContext context)
    {
        return new CodeArtifact.Credentials(toUsername(context), toPassword(context));
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
