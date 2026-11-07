package com.atleta.demo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuration for optional AI capabilities. No remote provider is enabled by default. */
@ConfigurationProperties(prefix = "atleta.ai")
public class AiProperties {
    private boolean enabled = false;
    private String provider = "disabled";
    private Duration timeout = Duration.ofSeconds(8);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
