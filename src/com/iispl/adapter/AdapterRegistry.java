package com.iispl.adapter;




import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.iispl.enums.SourceType;

/**
 * Central registry — maps each SourceType to its TransactionAdapter.
 * Adding a new source system: register one new adapter here.
 */
public class AdapterRegistry {

    private final Map<SourceType, TransactionAdapter> registry = new EnumMap<>(SourceType.class);

    public void register(TransactionAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("Adapter must not be null");
        registry.put(adapter.getSourceType(), adapter);
        System.out.printf("  [Registry] Registered adapter: %-8s%n", adapter.getSourceType());
    }

    public TransactionAdapter getAdapter(SourceType type) throws AdapterException {
        TransactionAdapter adapter = registry.get(type);
        if (adapter == null)
            throw new AdapterException(type.name(), "No adapter registered for: " + type);
        return adapter;
    }

    public boolean hasAdapter(SourceType type) { return registry.containsKey(type); }

    public Map<SourceType, TransactionAdapter> getAll() {
        return Collections.unmodifiableMap(registry);
    }

    public int size() { return registry.size(); }
}
