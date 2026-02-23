package ubc.cosc322;
//Shooting step class
// call every turn after moveStep
public final class ShootStep {
    private final int square;

    public ShootStep(int square) {
        this.square = square;
    }

    public int square() { return square; }

    @Override
    public String toString() {
        return "Shot(" + square + ")";
    }
}
