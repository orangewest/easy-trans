package io.github.orangewest.trans.spring.cfg;

import io.github.orangewest.trans.service.TransService;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easy-trans")
public class EasyTransProperties {

    /**
     * Enable parallel execution when DTO has 2+ repository groups (default {@code true}).
     * Set to {@code false} if your {@code TransRepository.getTransValueMap} depends on
     * {@link ThreadLocal} context (JPA session, Spring Security, MDC trace-id, etc.).
     */
    private boolean parallelRepoGroups = true;

    /**
     * Split a repository query into batches of at most this many keys
     * (default {@link TransService#DEFAULT_REPO_BATCH_SIZE}). Keeps queries under backend limits
     * (Oracle {@code IN} 1000, MySQL packet size, RPC batch caps) when translating large lists;
     * the framework splits keys and merges results, repositories stay unaware of chunking.
     * Set to {@code 0} (or negative) to disable splitting.
     */
    private int repoBatchSize = TransService.DEFAULT_REPO_BATCH_SIZE;

    public boolean isParallelRepoGroups() {
        return parallelRepoGroups;
    }

    public void setParallelRepoGroups(boolean parallelRepoGroups) {
        this.parallelRepoGroups = parallelRepoGroups;
    }

    public int getRepoBatchSize() {
        return repoBatchSize;
    }

    public void setRepoBatchSize(int repoBatchSize) {
        this.repoBatchSize = repoBatchSize;
    }

}
