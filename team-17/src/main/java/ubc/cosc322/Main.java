package ubc.cosc322;
import java.util.List;
import java.util.Scanner;

/*
  RUN FROM THIS FILE TO PLAY GAME

 Move format:  <from> <to> <arrow>   using square names, e.g.: j4 g7 e5
  Columns left-to-right: j i h g f e d c b a
 Rows top-to-bottom:    1 … 10
 
 TODOS:
  1) ai needs to be connected to the server
  2) right now the board doesn't turn when the ai is playing white, 
       we need to double check how the board is oriented on each side
       once it can connect to the server
  
  3) I tested the game against the bots on this site: https://abstractboardgames.com/amazons/play
       with me acting as the intermediary. Our ai plays as black, online bot plays as white. 
       right now it can beat all of the beginner level bots, but can't beat the lowest level intermediate leve
       
  4) I set up the board to print as the board on the bot site I was testing against,
      we will need to check that this is the same as the server board
 */
public class Main {

    // Column index 0..9  →  letter j i h g f e d c b a
    private static final String COLS = "jihgfedcba";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Determine which side the AI plays (can play as black or white)
        // TODO: board needs to reorient when AI is white, right now it's upside down when ai is white
        // TODO: once server is connected, print statements should be deleted and this section should be the 
        //       api hook up. (server will tell ai which side it is playing).
        Player aiColour;
        if (args.length > 0) {
            aiColour = args[0].equalsIgnoreCase("white") ? Player.WHITE : Player.BLACK;
        } else {
            System.out.print("Which colour is the ai playing? (white or black): ");
            String input = scanner.nextLine().trim().toLowerCase();
            aiColour = input.equals("white") ? Player.WHITE : Player.BLACK;
        }
        System.out.println("AI plays: " + aiColour + "   You play: " + aiColour.opponent());

        MonteCarloPicker picker = new MonteCarloPicker();
        GameState state = GameState.createInitial();

        // Prints out the board for testing
        // TODO: once server is connected we can delete this
        System.out.println("\n=== Initial position ===");
        System.out.println(state);
        System.out.println("Enter moves as:  <from> <to> <arrow>  e.g.  j4 g7 e5");



        // Main game loop. BE CAREFUL IF CHANGING THIS
        int turn = 1;
        while (!state.isTerminal()) {
            //TODO: once server is connected, we can remove this print statement too
            System.out.println("\n--- Turn " + turn + " (" + state.toMove + " to move) ---");

            FullTurn move;
            if (state.toMove == aiColour) {
                move = doAiTurn(picker, state);
            } else {
                move = doOpponentTurn(scanner, state);
            }

            GameState afterMove = state.applyMove(new MoveStep(move.from(), move.to()));
            state = afterMove.shoot(move.to(), new ShootStep(move.arrow()));

            System.out.println(state); // TODO: remove after server connection (double check that the server will send updated game state)
            turn++;
        }

        Player winner = state.toMove.opponent();
        //TODO: remove print statements once server is connected
        System.out.println("\nGame over — " + winner + " wins!"
                + (winner == aiColour ? "  (AI wins)" : "  (You win!)"));

        scanner.close();
    }

    private static FullTurn doAiTurn(MonteCarloPicker picker, GameState state) {
        System.out.println("AI is thinking..."); //TODO: remove after server connected
        long start = System.currentTimeMillis();
        FullTurn move = picker.pickMove(state);
        long ms = System.currentTimeMillis() - start;
        //TODO: remove after server connected
        System.out.printf("AI played: %s -> %s, arrow -> %s  (%d ms)%n",
                sq(move.from()), sq(move.to()), sq(move.arrow()), ms);
        return move;
    }

    //TODO: this just takes in the humans move, so we can remove this entire statement after server is connected.
    private static FullTurn doOpponentTurn(Scanner scanner, GameState state) { 
        while (true) {
            System.out.print("Your move (from to arrow): "); 
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                System.out.println("  Need exactly 3 squares, e.g.:  j4 g7 e5");
                continue;
            }

            int from  = parseSquare(parts[0]);
            int to    = parseSquare(parts[1]);
            int arrow = parseSquare(parts[2]);

            if (from == -1)  { System.out.println("  Can't read square: " + parts[0]); continue; }
            if (to   == -1)  { System.out.println("  Can't read square: " + parts[1]); continue; }
            if (arrow == -1) { System.out.println("  Can't read square: " + parts[2]); continue; }

            String error = validateMove(state, from, to, arrow);
            if (error != null) {
                System.out.println("  Illegal move: " + error);
                continue;
            }

            return new FullTurn(from, to, arrow);
        }
    }

    // We might be able to remove this after server connection as well, since Dr. Gao
    // said we don't need to do move validation on our side
    // it's useful for testing though
    private static String validateMove(GameState state, int from, int to, int arrow) {
        // Check the 'from' square holds one of our amazons
        int[] amazons = (state.toMove == Player.WHITE) ? state.whiteAmazons : state.blackAmazons;
        boolean ownsFrom = false;
        for (int sq : amazons) if (sq == from) { ownsFrom = true; break; }
        if (!ownsFrom) return "no friendly amazon at " + sq(from);

        // Check the amazon can legally slide to 'to'
        List<Integer> legalMoves = MoveGen.genQueenMoves(state.board, from);
        if (!legalMoves.contains(to))
            return "amazon at " + sq(from) + " cannot reach " + sq(to);

        // Check the arrow can legally reach 'arrow' from 'to'
        GameState afterMove = state.applyMove(new MoveStep(from, to));
        List<Integer> legalShots = MoveGen.genQueenShotsAfterMove(afterMove.board, from, to);
        if (!legalShots.contains(arrow))
            return "cannot shoot to " + sq(arrow) + " from " + sq(to);

        return null; // valid
    }

    /* Converts a square name like "j1" or "f5" to its flat board index (0-99).
       Returns -1 if the input is not a valid square name. 
      TODO: update this to match server square naming
      actually we might not even need this after server connection. need to double check docs
    */
    private static int parseSquare(String s) {
        s = s.toLowerCase().trim();
        if (s.length() < 2) return -1;
        int colIdx = COLS.indexOf(s.charAt(0));
        if (colIdx < 0) return -1;
        int row;
        try { row = Integer.parseInt(s.substring(1)); }
        catch (NumberFormatException e) { return -1; }
        if (row < 1 || row > 10) return -1;
        return (row - 1) * 10 + colIdx;
    }

    // Converts a flat board index (0-99) to its square name, e.g. 0 → "j1", 44 → "f5".
    private static String sq(int index) {
        return "" + COLS.charAt(index % 10) + (index / 10 + 1);
    }
}
