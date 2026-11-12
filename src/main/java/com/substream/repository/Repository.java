package com.substream.repository;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import com.substream.domain.Identifiable;

import java.util.List;
import java.util.Optional;

public interface Repository<T extends Identifiable<ID>, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    long count();
}
