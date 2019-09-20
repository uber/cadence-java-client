package com.uber.cadence.internal.common;

import com.uber.cadence.QueryRejected;

public final class QueryResponse<T> {
    private QueryRejected queryRejected;
    private T result;

    /**
     * Returns the value of the QueryRejected property for this object.
     *
     * @return The value of the QueryRejected property for this object.
     */
    public QueryRejected getQueryRejected() {
        return queryRejected;
    }

    /**
     * Sets the value of the QueryRejected property for this object.
     *
     * @param queryRejected The new value for the QueryRejected property for this object.
     */
    public void setQueryRejected(QueryRejected queryRejected) {
        this.queryRejected = queryRejected;
    }

    /**
     * Sets the value of the QueryRejected property for this object.
     *
     * <p>Returns a reference to this object so that method calls can be chained together.
     *
     * @param queryRejected The new value for the QueryRejected property for this object.
     * @return A reference to this updated object so that method calls can be chained together.
     */
    public QueryResponse<T> withQueryRejected(QueryRejected queryRejected) {
        this.queryRejected = queryRejected;
        return this;
    }

    /**
     * Returns the value of the Result property for this object.
     *
     * @return The value of the Result property for this object.
     */
    public T getResult() {
        return result;
    }

    /**
     * Sets the value of the Result property for this object.
     *
     * @param result The new value for the Result property for this object.
     */
    public void setResult(T result) {
        this.result = result;
    }

    /**
     * Sets the value of the Result property for this object.
     *
     * <p>Returns a reference to this object so that method calls can be chained together.
     *
     * @param result The new value for the Result property for this object.
     * @return A reference to this updated object so that method calls can be chained together.
     */
    public QueryResponse<T> withResult(T result) {
        this.result = result;
        return this;
    }
}
