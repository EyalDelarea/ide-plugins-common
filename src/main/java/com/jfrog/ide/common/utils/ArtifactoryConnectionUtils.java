package com.jfrog.ide.common.utils;

import com.jfrog.ide.common.configuration.ServerConfig;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Represents connection utils for Artifactory.
 *
 * @author yahavi
 */
public class ArtifactoryConnectionUtils {

    public static ArtifactoryManagerBuilder createArtifactoryManagerBuilder(ServerConfig serverConfig, Log logger) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = serverConfig.isInsecureTls() ?
                SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build() :
                serverConfig.getSslContext();
        return createAnonymousAccessArtifactoryManagerBuilder(serverConfig.getArtifactoryUrl(), serverConfig.getProxyConfForTargetUrl(serverConfig.getArtifactoryUrl()), logger)
                .setUsername(serverConfig.getUsername())
                .setPassword(serverConfig.getPassword())
                .setAccessToken(serverConfig.getAccessToken())
                .setSslContext(sslContext);
    }

    public static ArtifactoryManagerBuilder createAnonymousAccessArtifactoryManagerBuilder(String url, ProxyConfiguration proxyConfiguration, Log logger) {
        return new ArtifactoryManagerBuilder()
                .setServerUrl(url)
                .setProxyConfiguration(proxyConfiguration)
                .setLog(logger);
    }
}
