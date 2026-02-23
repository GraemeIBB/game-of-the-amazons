package ubc.cosc322;
// A complete turn: move the amazon, then fire an arrow 

public final class FullTurn {
    private final int from;
    private final int to;
    private final int arrow;

    public FullTurn(int from, int to, int arrow) {
        this.from  = from;
        this.to    = to;
        this.arrow = arrow;
    }

    public int from()  { return from;  }
    public int to()    { return to;    }
    public int arrow() { return arrow; }

    @Override
    public String toString() {
        return "FullMove(" + from + "->" + to + ", arrow=" + arrow + ")";
    }
}
