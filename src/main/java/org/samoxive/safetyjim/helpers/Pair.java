package org.samoxive.safetyjim.helpers;

import lombok.Getter;

@Getter
public class Pair<A, B> {
    private A left;
    private B right;

    public Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }


    public static <A, B> Pair<A,B> of(A left, B right) {
        return new Pair<>(left, right);
    }
}
