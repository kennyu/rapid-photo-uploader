package com.rapidphoto.uploader.application;

/**
 * Base interface for all query handlers in the CQRS pattern.
 * Query handlers execute queries and return data without modifying state.
 *
 * @param <Q> The query type to handle
 * @param <R> The result type returned after handling
 */
public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}

