package com.playmotech.api.core.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Organisation;
import com.playmotech.api.core.dao_postgres.OrganisationConfig;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.repo.OrgRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AcademyConfigCache {
    // Redis keys
    private static final String DEFAULT_CONFIG_KEY = "config:default";
    private static final String ORG_CONFIG_PREFIX = "config:org:";
    private static final String MERGED_CONFIG_PREFIX = "config:merged:academy:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrgRepo orgRepo;

    @Value("${default.org.config.url}")
    private String defaultOrgConfigUrl;

    @Value("${config.cache.default.ttl}") // fallback to 30 minutes (1800 seconds)
    private long defaultConfigTtl;

    @Value("${config.cache.org.ttl}") // fallback to 10 minutes (600 seconds)
    private long orgConfigTtl;

    @Value("${config.cache.merged.ttl}") // fallback to 5 minutes (300 seconds)
    private long mergedConfigTtl;

    public AcademyConfigCache(final OrgRepo orgRepo, RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.orgRepo = orgRepo;
    }

    /**
     * Main method - gets merged config with Redis caching at all levels
     */
    public void setOrgConfigOrDefault(AcademyDto academyDto, Academy academy) {
        String mergedCacheKey = MERGED_CONFIG_PREFIX + academy.getId();

        // Try to get merged config from Redis first
        Map<String, String> cachedMergedConfig = getCachedMergedConfig(mergedCacheKey);
        if (cachedMergedConfig != null) {
            log.debug("Using cached merged config for academy: {}", academy.getId());
            academyDto.setConfig(cachedMergedConfig);
            return;
        }

        // Cache miss - build merged config using cached components
        log.info("Building merged config for academy: {}", academy.getId());
        Map<String, String> finalConfig = new HashMap<>(getDefaultConfig());

        if (academy.getOrg() != null && academy.getOrg().getId() != null) {
            Map<String, String> orgConfig = getOrgConfig(academy.getOrg().getId());
            if (!orgConfig.isEmpty()) {
                log.info("org config: {}", orgConfig);
                finalConfig.putAll(orgConfig);
            }
        }

        // Cache the merged result
        cacheMergedConfig(mergedCacheKey, finalConfig);

        log.info("Final merged config: {}", finalConfig);
        academyDto.setConfig(finalConfig);
    }

    /**
     * Get default config from Redis cache or S3
     */
    public Map<String, String> getDefaultConfig() {
        try {
            // Try Redis first
            @SuppressWarnings("unchecked")
            Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(DEFAULT_CONFIG_KEY);

            if (cached != null && !cached.isEmpty()) {
                log.debug("Retrieved default config from Redis cache");
                return cached;
            }

            // Load from S3 and cache
            log.info("Loading default config from S3");
            Map<String, String> freshConfig = loadDefaultConfigFromS3();

            if (!freshConfig.isEmpty()) {
                redisTemplate.opsForValue().set(DEFAULT_CONFIG_KEY, freshConfig, Duration.ofSeconds(defaultConfigTtl));
                log.info("Cached default config in Redis with {} entries", freshConfig.size());
            }

            return freshConfig;

        } catch (Exception e) {
            log.error("Error with Redis cache, falling back to S3", e);
            return loadDefaultConfigFromS3();
        }
    }

    /**
     * Get organization config from Redis cache or database
     */
    public Map<String, String> getOrgConfig(String orgId) {
        String cacheKey = ORG_CONFIG_PREFIX + orgId;

        try {
            // Try Redis first
            @SuppressWarnings("unchecked")
            Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(cacheKey);

            if (cached != null) {
                log.debug("Retrieved org config from Redis cache for orgId: {}", orgId);
                return cached;
            }

            // Load from database and cache
            log.info("Loading org config from database for orgId: {}", orgId);
            Map<String, String> orgConfig = loadOrgConfigFromDatabase(orgId);

            if (!orgConfig.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, orgConfig, Duration.ofSeconds(orgConfigTtl));
                log.info("Cached org config in Redis for orgId: {} with {} entries", orgId, orgConfig.size());
            } else {
                // Cache empty result with shorter TTL to avoid repeated DB calls
                redisTemplate.opsForValue().set(cacheKey, new HashMap<>(), Duration.ofSeconds(60));
            }

            return orgConfig;

        } catch (Exception e) {
            log.error("Error with Redis cache for org config, falling back to database", e);
            return loadOrgConfigFromDatabase(orgId);
        }
    }

    /**
     * Get cached merged config from Redis
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getCachedMergedConfig(String cacheKey) {
        try {
            Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? cached : null;
        } catch (Exception e) {
            log.warn("Error retrieving cached merged config: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * Cache merged config in Redis
     */
    private void cacheMergedConfig(String cacheKey, Map<String, String> config) {
        try {
            redisTemplate.opsForValue().set(cacheKey, config, Duration.ofSeconds(mergedConfigTtl));
            log.debug("Cached merged config: {} with {} entries", cacheKey, config.size());
        } catch (Exception e) {
            log.error("Error caching merged config: {}", cacheKey, e);
        }
    }

    /**
     * Load default config from S3
     */
    private Map<String, String> loadDefaultConfigFromS3() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(
                    defaultOrgConfigUrl + "?v=" + System.currentTimeMillis(),
                    String.class);

            if (json == null || json.trim().isEmpty()) {
                log.warn("Empty response from S3 config URL");
                return new HashMap<>();
            }

            Gson gson = new Gson();
            Map<String, String> config = gson.fromJson(json, new TypeToken<Map<String, String>>() {
            }.getType());
            log.info("Loaded {} default config entries from S3", config.size());
            return config != null ? config : new HashMap<>();

        } catch (Exception e) {
            log.error("Failed to load default config from S3: {}", defaultOrgConfigUrl, e);
            return new HashMap<>();
        }
    }

    /**
     * Load organization config from database
     */
    private Map<String, String> loadOrgConfigFromDatabase(String orgId) {
        try {
            Organisation org = orgRepo.findById(orgId).orElse(null);
            if (org == null || org.getConfigs() == null || org.getConfigs().isEmpty()) {
                log.debug("No config found for orgId: {}", orgId);
                return new HashMap<>();
            }

            Map<String, String> orgConfig = org.getConfigs().stream()
                    .collect(Collectors.toMap(
                            OrganisationConfig::getKey,
                            OrganisationConfig::getValue,
                            (existing, replacement) -> replacement // Handle duplicate keys
                    ));

            log.debug("Loaded {} org config entries from database for orgId: {}", orgConfig.size(), orgId);
            return orgConfig;

        } catch (Exception e) {
            log.error("Error loading org config from database for orgId: {}", orgId, e);
            return new HashMap<>();
        }
    }

    // ================== CACHE MANAGEMENT METHODS ==================

    /**
     * Invalidate default config cache
     */
    public void invalidateDefaultConfig() {
        try {
            redisTemplate.delete(DEFAULT_CONFIG_KEY);
            log.info("Invalidated default config cache");
        } catch (Exception e) {
            log.error("Error invalidating default config cache", e);
        }
    }

    /**
     * Invalidate organization config cache
     */
    public void invalidateOrgConfig(String orgId) {
        try {
            String cacheKey = ORG_CONFIG_PREFIX + orgId;
            redisTemplate.delete(cacheKey);
            log.info("Invalidated org config cache for orgId: {}", orgId);

            // Also invalidate all merged configs that might use this org config
            invalidateMergedConfigsByOrg(orgId);

        } catch (Exception e) {
            log.error("Error invalidating org config cache for orgId: {}", orgId, e);
        }
    }

    /**
     * Invalidate merged config for specific academy
     */
    public void invalidateMergedConfig(Long academyId) {
        try {
            String cacheKey = MERGED_CONFIG_PREFIX + academyId;
            redisTemplate.delete(cacheKey);
            log.info("Invalidated merged config cache for academyId: {}", academyId);
        } catch (Exception e) {
            log.error("Error invalidating merged config cache for academyId: {}", academyId, e);
        }
    }

    /**
     * Invalidate all merged configs for academies belonging to an organization
     */
    private void invalidateMergedConfigsByOrg(String orgId) {
        try {
            String pattern = MERGED_CONFIG_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} merged config entries for org change", keys.size());
            }
        } catch (Exception e) {
            log.error("Error invalidating merged configs for orgId: {}", orgId, e);
        }
    }

    /**
     * Clear all configuration caches
     */
    public void clearAllConfigCaches() {
        try {
            Set<String> allConfigKeys = new HashSet<>();

            // Find all config keys
            Set<String> defaultKeys = redisTemplate.keys(DEFAULT_CONFIG_KEY);
            Set<String> orgKeys = redisTemplate.keys(ORG_CONFIG_PREFIX + "*");
            Set<String> mergedKeys = redisTemplate.keys(MERGED_CONFIG_PREFIX + "*");

            if (defaultKeys != null)
                allConfigKeys.addAll(defaultKeys);
            if (orgKeys != null)
                allConfigKeys.addAll(orgKeys);
            if (mergedKeys != null)
                allConfigKeys.addAll(mergedKeys);

            if (!allConfigKeys.isEmpty()) {
                redisTemplate.delete(allConfigKeys);
                log.info("Cleared {} config cache entries", allConfigKeys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing all config caches", e);
        }
    }

    // ================== CACHE WARMING METHODS ==================

    /**
     * Warm cache for specific academy
     */
    public void warmCache(Academy academy) {
        try {
            log.info("Warming cache for academy: {}", academy.getId());

            // Warm default config
            getDefaultConfig();

            // Warm org config if academy has org
            if (academy.getOrg() != null && academy.getOrg().getId() != null) {
                getOrgConfig(academy.getOrg().getId());
            }

            // Build and cache merged config
            String mergedCacheKey = MERGED_CONFIG_PREFIX + academy.getId();
            Map<String, String> finalConfig = new HashMap<>(getDefaultConfig());

            if (academy.getOrg() != null && academy.getOrg().getId() != null) {
                Map<String, String> orgConfig = getOrgConfig(academy.getOrg().getId());
                finalConfig.putAll(orgConfig);
            }

            cacheMergedConfig(mergedCacheKey, finalConfig);
            log.info("Cache warmed for academy: {}", academy.getId());

        } catch (Exception e) {
            log.error("Error warming cache for academy: {}", academy.getId(), e);
        }
    }

    /**
     * Warm caches for multiple academies
     */
    @Async
    public CompletableFuture<Void> warmCachesAsync(List<Academy> academies) {
        log.info("Starting async cache warming for {} academies", academies.size());

        academies.parallelStream().forEach(academy -> {
            try {
                warmCache(academy);
            } catch (Exception e) {
                log.error("Error warming cache for academy: {}", academy.getId(), e);
            }
        });

        log.info("Completed cache warming for {} academies", academies.size());
        return CompletableFuture.completedFuture(null);
    }

    // ================== CACHE MONITORING METHODS ==================

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Count cached entries
            Set<String> orgKeys = redisTemplate.keys(ORG_CONFIG_PREFIX + "*");
            Set<String> mergedKeys = redisTemplate.keys(MERGED_CONFIG_PREFIX + "*");

            stats.put("default_config_cached", redisTemplate.hasKey(DEFAULT_CONFIG_KEY));
            stats.put("org_configs_count", orgKeys != null ? orgKeys.size() : 0);
            stats.put("merged_configs_count", mergedKeys != null ? mergedKeys.size() : 0);

            // Check TTLs
            stats.put("default_config_ttl", redisTemplate.getExpire(DEFAULT_CONFIG_KEY, TimeUnit.SECONDS));

        } catch (Exception e) {
            log.error("Error getting cache stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Check if specific configs are cached
     */
    public Map<String, Boolean> checkCacheStatus(Long academyId, Long orgId) {
        Map<String, Boolean> status = new HashMap<>();

        try {
            status.put("default_cached", Boolean.TRUE.equals(redisTemplate.hasKey(DEFAULT_CONFIG_KEY)));

            if (orgId != null) {
                String orgKey = ORG_CONFIG_PREFIX + orgId;
                status.put("org_cached", Boolean.TRUE.equals(redisTemplate.hasKey(orgKey)));
            }

            if (academyId != null) {
                String mergedKey = MERGED_CONFIG_PREFIX + academyId;
                status.put("merged_cached", Boolean.TRUE.equals(redisTemplate.hasKey(mergedKey)));
            }

        } catch (Exception e) {
            log.error("Error checking cache status", e);
            status.put("error", true);
        }

        return status;
    }
}
