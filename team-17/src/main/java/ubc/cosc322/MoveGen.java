package ubc.cosc322;
import java.util.ArrayList;
import java.util.List;

/*
 Generates legal moves and arrow shots
 Board layout: flat int[100], index = row * 10 + col  (row 0 = top).
 Cell values: 0 = empty, 1 = white amazon, 2 = black amazon, 3 = arrow.
 */
public final class MoveGen {
    public static final int SIZE = 10;

    // 8 queen directions as delta row (dr) and delta column (dc) pairs
    // for dr (neg is up, pos is down) and dc (neg is left, pos is right)
    private static final int[] DR = {-1, -1, -1,  0,  0,  1,  1,  1};
    private static final int[] DC = {-1,  0,  1, -1,  1, -1,  0,  1};

    private MoveGen() {}

    /*
     All squares reachable from current location
     Slides stop at (but do not include) any non-empty square.
     */
    public static List<Integer> genQueenMoves(int[] board, int from) {
        List<Integer> result = new ArrayList<>(30);
        int row = from / SIZE;
        int col = from % SIZE;
        for (int d = 0; d < 8; d++) {
            int r = row + DR[d];
            int c = col + DC[d];
            while (r >= 0 && r < SIZE && c >= 0 && c < SIZE) {
                int sq = r * SIZE + c;
                if (board[sq] != 0) break; // blocked
                result.add(sq);
                r += DR[d];
                c += DC[d];
            }
        }
        return result;
    }

     // returns all squares where the arrow can land after the amazon moved 
    public static List<Integer> genQueenShotsAfterMove(int[] board, int from, int to) {
        return genQueenMoves(board, to);
    }
}
