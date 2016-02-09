package io.paradoxical.cassieq.configurations.cassandra;

import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.SSLOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Data
public class Ssl {

    @NotNull
    @JsonProperty
    private Integer port;

    public SSLOptions build() throws NoSuchAlgorithmException {
        initSSLContext();

        return JdkSSLOptions.builder()
                            .withSSLContext(SSLContext.getDefault())
                            .withCipherSuites(SSLContext.getDefault().getSocketFactory().getSupportedCipherSuites())
                            .build();
    }

    private void initSSLContext() {
        TrustManager[] certs = new TrustManager[]{ new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }
        } };

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, certs, new SecureRandom());
            SSLContext.setDefault(context);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
