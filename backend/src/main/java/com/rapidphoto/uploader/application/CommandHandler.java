package com.rapidphoto.uploader.application;

/**
 * Base interface for all command handlers in the CQRS pattern.
 * Command handlers execute commands and modify state.
 *
 * @param <C> The command type to handle
 * @param <R> The result type returned after handling
 */
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

