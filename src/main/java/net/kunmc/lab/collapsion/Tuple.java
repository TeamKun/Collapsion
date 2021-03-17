package net.kunmc.lab.collapsion;

public class Tuple<A,B> {
    private A left;
    private B right;
    public Tuple(A left, B right){
        this.left = left;
        this.right = right;
    }

    public A getLeft() {
        return left;
    }

    public B getRight() {
        return right;
    }
}
