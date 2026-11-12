package com.substream.repository;

import com.substream.domain.Identifiable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * M2. Generic in-memory repository (README, section 6).
 * {@code save} stores the entity under its id and returns it; saving an entity with an existing id replaces
 * the stored one, which keeps its place in the order. {@link #findAll()} returns the entities in insertion
 * order, as a list that cannot be modified. {@code findById} of an unknown id is an empty Optional.
 *
 * TODO: choose a suitable collection and store it in a private field.
 */
public class InMemoryRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {

    private final Map<ID, T> entities = new LinkedHashMap<>();

    @Override
    public T save(T entity) {
        // TODO: M2
        entities.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        // TODO: M2
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public List<T> findAll() {
        // TODO: M2
        return List.copyOf(entities.values());
    }

    @Override
    public long count() {
        // TODO: M2
        return entities.size();
    }
}
