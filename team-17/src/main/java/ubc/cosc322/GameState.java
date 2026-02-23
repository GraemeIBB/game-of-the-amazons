package ubc.cosc322;
/*
  Board encoding
  0 = empty
  1 = white amazon
  2 = black amazon
  3 = arrow (burned square)
  
  Standard starting positions
  White amazons: d1, g1, a4, j4  →  squares  3,  6, 30, 39
  Black amazons: a7, j7, d10, g10 →  squares 60, 69, 93, 96
  
  TODO: update square encoding and naming to match server board
  TODO: double check if we need to keep track of amazon positions separately after server connection 
*/

public final class GameState {

    // Board cell constants
    public static final int EMPTY = 0;
    public static final int WHITE = 1;
    public static final int BLACK = 2;
    public static final int ARROW = 3;

    //Flat 10×10 board
    public final int[] board;

    // Positions queens
    public final int[] whiteAmazons;
    public final int[] blackAmazons;

    //The player whose turn it is to move.
    public final Player toMove;

    public GameState(int[] board, int[] whiteAmazons, int[] blackAmazons, Player toMove) {
        this.board = board;
        this.whiteAmazons = whiteAmazons;
        this.blackAmazons = blackAmazons;
        this.toMove = toMove;
    }

    //Returns a fresh game in the standard opening position, white to move
    public static GameState createInitial() {
        int[] board = new int[100];

        int[] white = {3, 6, 30, 39};   // d1, g1, a4, j4
        int[] black = {60, 69, 93, 96}; // a7, j7, d10, g10

        for (int sq : white) board[sq] = WHITE;
        for (int sq : black) board[sq] = BLACK;

        return new GameState(board, white.clone(), black.clone(), Player.WHITE);
    }


    // Returns true if the player to move has no legal amazon moves
    public boolean isTerminal() {
        int[] amazons = (toMove == Player.WHITE) ? whiteAmazons : blackAmazons;
        for (int pos : amazons) {
            if (!MoveGen.genQueenMoves(board, pos).isEmpty()) return false;
        }
        return true;
    }

    //move step. returns new GameState with the updated board and amazon positions
    public GameState applyMove(MoveStep move) {
        int[] newBoard = board.clone();
        int piece = newBoard[move.from()];
        newBoard[move.from()] = EMPTY;
        newBoard[move.to()]   = piece;

        int[] newWhite = whiteAmazons.clone();
        int[] newBlack = blackAmazons.clone();

        if (toMove == Player.WHITE) {
            for (int i = 0; i < newWhite.length; i++) {
                if (newWhite[i] == move.from()) { newWhite[i] = move.to(); break; }
            }
        } else {
            for (int i = 0; i < newBlack.length; i++) {
                if (newBlack[i] == move.from()) { newBlack[i] = move.to(); break; }
            }
        }

        return new GameState(newBoard, newWhite, newBlack, toMove);
    }

    
     //shoot step
    public GameState shoot(int amazonTo, ShootStep shot) {
        int[] newBoard = board.clone();
        newBoard[shot.square()] = ARROW;
        return new GameState(newBoard, whiteAmazons.clone(), blackAmazons.clone(), toMove.opponent());
    }

    // Column labels left-to-right: j i h g f e d c b a
    //TODO: update this to match server square naming
   // private static final char[] COLUMN_LABELS = {'j','i','h','g','f','e','d','c','b','a'};

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Column header
        sb.append("    j  i  h  g  f  e  d  c  b  a\n");
        for (int r = 0; r < 10; r++) {
            // Row label 1-10, right-aligned in 2 chars
            sb.append(String.format("%2d  ", r + 1));
            for (int c = 0; c < 10; c++) {
                int v = board[r * 10 + c];
                sb.append(v == EMPTY ? '.' : v == WHITE ? 'W' : v == BLACK ? 'B' : 'X');
                if (c < 9) sb.append("  ");
            }
            sb.append('\n');
        }
        sb.append("To move: ").append(toMove);
        return sb.toString();
    }
}
