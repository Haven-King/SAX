package dev.hephaestus.sax.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;

import java.util.List;

public class ListView<T> {
    private static final Object2ObjectAVLTreeMap<List<?>, ListView<?>> CACHED_VIEWS = new Object2ObjectAVLTreeMap<>();

    private final List<T> wrapped;

    private ListView(List<T> wrapped) {
        this.wrapped = wrapped;
    }

    public int size() {
        return this.wrapped.size();
    }

    public T get(int index) {
        return this.wrapped.get(index);
    }

    @SuppressWarnings("unchecked")
    public static <T> ListView<T> of(List<T> list) {
        return (ListView<T>) CACHED_VIEWS.computeIfAbsent(list, ListView::new);
    }
}
