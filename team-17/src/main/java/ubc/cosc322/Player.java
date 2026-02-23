package ubc.cosc322;
// basic player class
public enum Player {
    WHITE, BLACK;

    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
