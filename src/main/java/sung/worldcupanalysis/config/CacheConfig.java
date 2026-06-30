package sung.worldcupanalysis.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caches. Team analyses and matchup previews are cached so we don't
 * re-call (and re-pay for) the Claude API on every page view; fixtures are
 * cached and evicted periodically by {@code MatchService}.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("worldCupMatches", "teamAnalysis", "matchupPreview");
    }
}
