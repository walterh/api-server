package com.llug.api.persistence.model;

import java.io.Serializable;
import java.util.List;

public interface EntityOperations<T extends Serializable> {

    T findOne(final long id);

    List<T> findAll();

    void create(final T entity);

    T update(final T entity);

    void delete(final T entity);

    void deleteById(final long entityId);

    List<T> queryNative(final String query);

    List<T> queryIn(final String query);
}
