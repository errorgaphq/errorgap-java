package com.errorgap;

/**
 * The APM transaction running on the current thread, so errors reported while
 * it runs carry its id as {@code context.transaction_id} and errorgap links
 * the error to the request or job that raised it.
 *
 * <pre>{@code
 * ApmTransaction transaction = new ApmTransaction().setMethod("GET").setPath("/orders/{id}");
 * try (TransactionContext.Scope scope = TransactionContext.enter(transaction.getId())) {
 *     handle(request);   // errors reported here carry transaction.getId()
 * }
 * client.notifyTransaction(transaction.setStatusCode(200));
 * }</pre>
 *
 * Thread-local: a servlet container serves each request on one thread, so
 * concurrent requests never share an id. Work handed to another thread does
 * not inherit it; enter a scope there too.
 */
public final class TransactionContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TransactionContext() {}

    /** The id of the transaction running on this thread, or {@code null}. */
    public static String current() {
        return CURRENT.get();
    }

    /** Make {@code id} current until the returned scope is closed; the previous id is restored then. */
    public static Scope enter(String id) {
        String previous = CURRENT.get();
        CURRENT.set(id);
        return new Scope(previous);
    }

    /** Restores the id that was current before {@link #enter(String)}. */
    public static final class Scope implements AutoCloseable {
        private final String previous;
        private boolean closed;

        private Scope(String previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
