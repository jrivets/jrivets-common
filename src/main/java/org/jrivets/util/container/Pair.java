package org.jrivets.util.container;

import org.jrivets.util.Objects;

public final class Pair<A, B> {
    
    private final A first;
    
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Objects.hashCode(1, first), second);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair<?, ?> op = (Pair<?, ?>) obj;
            return Objects.equal(first, op.first) && Objects.equal(second, op.second);
        }
        return false;
    }

    @Override
    public String toString() {
        return "{first=" + first + ", second=" + second + "}";
    }
}
