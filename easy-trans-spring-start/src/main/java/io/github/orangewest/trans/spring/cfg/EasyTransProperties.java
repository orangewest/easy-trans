package io.github.orangewest.trans.spring.cfg;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easy-trans")
public class EasyTransProperties {

    /**
     * Enable parallel execution when DTO has 2+ repository groups (default {@code true}).
     * Set to {@code false} if your {@code TransRepository.getTransValueMap} depends on
     * {@link ThreadLocal} context (JPA session, Spring Security, MDC trace-id, etc.).
     */
    private boolean parallelRepoGroups = true;

    public boolean isParallelRepoGroups() {
        return parallelRepoGroups;
    }

    public void setParallelRepoGroups(boolean parallelRepoGroups) {
        this.parallelRepoGroups = parallelRepoGroups;
    }

}
