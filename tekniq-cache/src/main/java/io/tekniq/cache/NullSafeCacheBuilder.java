package io.tekniq.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.function.Function;

/**
 * Bridges Caffeine's LoadingCache with Kotlin's strict null-safety for generic type parameters.
 * Kotlin 2.3+ enforces non-null bounds on Caffeine's type parameters, preventing nullable
 * returns from the cache loader lambda. This Java helper bypasses that restriction since
 * Caffeine's CacheLoader.load() natively supports null returns to signal "do not cache".
 */
public final class NullSafeCacheBuilder {
    private NullSafeCacheBuilder() {}

    @SuppressWarnings("unchecked")
    static <K, V> LoadingCache<K, V> build(Caffeine<Object, Object> builder, Function<K, V> loader) {
        return builder.build(loader::apply);
    }
}
