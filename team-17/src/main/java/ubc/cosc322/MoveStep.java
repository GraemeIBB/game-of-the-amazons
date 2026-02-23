package ubc.cosc322;
// basic move class
// call before shoot step every turn

public final class MoveStep {
    private final int from;
    private final int to;

    public MoveStep(int from, int to) {
        this.from = from;
        this.to   = to;
    }

    public int from() { return from; }
    public int to()   { return to;   }

    @Override
    public String toString() {
        return "Move(" + from + "->" + to + ")";
    }
}
