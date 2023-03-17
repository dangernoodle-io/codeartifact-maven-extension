package io.dangernoodle.codeartifact.maven.wagon;

import io.dangernoodle.codeartifact.maven.CodeArtifact;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;

import java.util.Optional;


/**
 * Maven <code>http</code> wagon implementation that handles authentication against <code>CodeArtifact</code>
 * prior to upload/download of artifacts.
 *
 * <p>This implementation is used by maven versions &lt;= 3.9.0</p>
 */
public class CodeArtifactWagon extends AbstractHttpClientWagon
{
    private final CodeArtifact codeArtifact;

    public CodeArtifactWagon()
    {
        this.codeArtifact = createCodeArtifact();
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws ConnectionException, AuthenticationException
    {
        if (codeArtifact.isCodeArtifactRepository(repository.getUrl()))
        {
            CodeArtifact.Credentials credentials = createCodeArtifactCredentials(repository.getHost(),
                    authenticationInfo);

            authenticationInfo = new AuthenticationInfo();
            authenticationInfo.setUserName(credentials.username);
            authenticationInfo.setPassword(credentials.password);
        }

        super.connect(repository, authenticationInfo, proxyInfoProvider);
    }

    // visible for testing
    CodeArtifact createCodeArtifact()
    {
        return new CodeArtifact();
    }

    private boolean areKeysSet(AuthenticationInfo authInfo)
    {
        return authInfo.getUserName() != null && authInfo.getPassword() != null;
    }

    private CodeArtifact.Credentials createCodeArtifactCredentials(String host, AuthenticationInfo authInfo)
    {
        CodeArtifact.Credentials credentials = createCredentials(authInfo);
        return codeArtifact.createCredentials(host, credentials);
    }

    private CodeArtifact.Credentials createCredentials(AuthenticationInfo authInfo)
    {
        return Optional.ofNullable(authInfo)
                       .filter(this::areKeysSet)
                       .map(this::toCredentials)
                       .orElse(CodeArtifact.Credentials.EMPTY);
    }

    private CodeArtifact.Credentials toCredentials(AuthenticationInfo authInfo)
    {
        return new CodeArtifact.Credentials(authInfo.getUserName(), authInfo.getPassword());
    }
}