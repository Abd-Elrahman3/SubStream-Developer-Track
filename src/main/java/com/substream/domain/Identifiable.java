package com.substream.domain;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

/**
 * Any entity that can be stored in a {@code Repository<T, ID>}.
 */
public interface Identifiable<ID> {

    ID getId();
}
