package ubc.cosc322;
import java.util.*;

public final class MonteCarloPicker {
    private final Random rng = new Random();

    private static final long THINK_TIME = 28_000; // total think time per move
    private static final double MOVE_FRACTION = 0.70; // fraction of budget for move tree (rest goes to shot tree)
    private static final int SHOT_TREE_BRANCHING = 20; // sampled full-move fan-out at deeper shot-tree levels
    private static final double UCT_C = Math.sqrt(2); //exploration constant UCT = (wins/visits) + C * sqrt(ln(parent_visits) / child_visits) TUNE BY CHANGING C. higher c = more exploration, lower c = more exploitation
    private static final int MAX_ROLLOUT_DEPTH = 300; // max number of turns in each simulation

    
    public FullTurn pickMove(GameState root) {
        if (root.isTerminal()) throw new IllegalStateException("No legal moves");
        Player us = root.toMove;

        // Phase 1: build the move tree and pick the best move
        MoveStep bestMove = runMoveMCTS(root, us);

        // Phase 2: apply that move, then build the shot tree and pick the best arrow
        GameState afterMove = root.applyMove(bestMove);
        int arrowSq = runShotMCTS(afterMove, bestMove.from(), bestMove.to(), us);

        return new FullTurn(bestMove.from(), bestMove.to(), arrowSq);
    }

    
    //Move Tree MCTS
    private MoveStep runMoveMCTS(GameState state, Player us) {
        List<MoveStep> moves = getAllMoveSteps(state);
        if (moves.isEmpty()) throw new IllegalStateException("No legal moves.");
        if (moves.size() == 1) return moves.get(0);

        Node root = new Node(null, state, moves);

        long moveDeadline = System.currentTimeMillis() + (long)(THINK_TIME * MOVE_FRACTION);
        while (System.currentTimeMillis() < moveDeadline) {

            //Selection: walk down with UCT
            Node node = root;
            while (node.untried.isEmpty() && !node.children.isEmpty()) {
                node = uctSelect(node);
            }

            // Expansion: try one untried move step
            if (!node.untried.isEmpty()) {
                MoveStep m = (MoveStep) removeRandom(node.untried);
                GameState after = node.state.applyMove(m);
                // Leaf nodes in the move tree get no more untried actions;
                // simulation completes the turn with a random shot.
                Node child = new Node(m, after, Collections.emptyList());
                child.parent = node;
                node.children.add(child);
                node = child;
            }

            // Simulation
            double score;
            if (node.parent == null) {
                // Root fully expanded with no children — shouldn't occur in practice
                score = 0.5;
            } else {
                // node.state is an after-move state; node.action is the MoveStep taken
                MoveStep m = (MoveStep) node.action;
                score = simulateFromAfterMove(node.state, m.from(), m.to(), us);
            }

            // Backpropagation
            for (Node n = node; n != null; n = n.parent) {
                n.visits++;
                n.wins += score;
            }
        }

        return (MoveStep) mostVisitedChild(root).action;
    }

    // Shoot Tree MCTS
    private int runShotMCTS(GameState afterMoveState, int amazonFrom, int amazonTo, Player us) {
        List<Integer> shots = MoveGen.genQueenShotsAfterMove(
                afterMoveState.board, amazonFrom, amazonTo);
        if (shots.isEmpty()) throw new IllegalStateException("No legal shots.");
        if (shots.size() == 1) return shots.get(0);

        // Root holds the after-move state; untried = all legal shot squares.
        Node root = new Node(null, afterMoveState, shots);

        long shotDeadline = System.currentTimeMillis() + (long)(THINK_TIME * (1.0 - MOVE_FRACTION));
        while (System.currentTimeMillis() < shotDeadline) {

            //Selection
            Node node = root;
            while (node.untried.isEmpty() && !node.children.isEmpty()) {
                node = uctSelect(node);
            }

            // Expansion
            if (!node.untried.isEmpty()) {
                if (node.parent == null) {
                    // Expanding the root: action is a shot square
                    int arrow = (Integer) removeRandom(node.untried);
                    GameState postShot = node.state.shoot(amazonTo, new ShootStep(arrow));
                    // Give the new child a sampled set of the opponent's full moves so the tree can grow deeper on subsequent iterations.
                    List<FullTurn> oppMoves = sampleRandomFullMoves(postShot, SHOT_TREE_BRANCHING);
                    Node child = new Node(arrow, postShot, oppMoves);
                    child.parent = node;
                    node.children.add(child);
                    node = child;
                } else {
                    // Expanding a deeper node: action is a FullMove
                    FullTurn fm = (FullTurn) removeRandom(node.untried);
                    GameState nextState = applyFullMove(node.state, fm);
                    List<FullTurn> nextMoves = sampleRandomFullMoves(nextState, SHOT_TREE_BRANCHING);
                    Node child = new Node(fm, nextState, nextMoves);
                    child.parent = node;
                    node.children.add(child);
                    node = child;
                }
            }

            // Simulation
            double score;
            if (node.parent == null) {
                // Rollout from root
                score = simulateFromAfterMove(node.state, amazonFrom, amazonTo, us);
            } else {
                score = rolloutToTerminal(node.state, us);
            }

            // Backpropagation
            for (Node n = node; n != null; n = n.parent) {
                n.visits++;
                n.wins += score;
            }
        }
        return (Integer) mostVisitedChild(root).action;
    }

    /*
     Complete a half-turn from an after-move state:
     pick a random arrow shot, then run a full random rollout.
     */
    private double simulateFromAfterMove(GameState afterMove, int from, int to, Player us) {
        List<Integer> shots = MoveGen.genQueenShotsAfterMove(afterMove.board, from, to);
        if (shots.isEmpty()) return 0.0;
        int arrow     = shots.get(rng.nextInt(shots.size()));
        GameState postShot = afterMove.shoot(to, new ShootStep(arrow));
        return rolloutToTerminal(postShot, us);
    }

    /*
     Random playout from state s.
     Returns 1.0 if ai wins, 0.0 if lose, 0.5 if max depth is hit without terminal state (treat as draw).
     */
    private double rolloutToTerminal(GameState s, Player us) {
        int plies = 0;
        while (!s.isTerminal() && plies < MAX_ROLLOUT_DEPTH) {
            s = applyFullMove(s, randomFullMove(s));
            plies++;
        }
        if (s.isTerminal()) {
            // Player to move has no moves
            Player winner = (s.toMove == Player.WHITE) ? Player.BLACK : Player.WHITE;
            return (winner == us) ? 1.0 : 0.0;
        }
        return 0.5; 
    }

    //UCT child selection
    private Node uctSelect(Node parent) {
        Node best = null;
        double bestVal = Double.NEGATIVE_INFINITY;
        double logP = Math.log(parent.visits);
        for (Node c : parent.children) {
            if (c.visits == 0) return c; // always try unvisited children first
            double val = (c.wins / c.visits) + UCT_C * Math.sqrt(logP / c.visits);
            if (val > bestVal) { bestVal = val; best = c; }
        }
        return best;
    }

    // Most-visited child. standard robust best-move criterion
    private Node mostVisitedChild(Node parent) {
        Node best = null;
        for (Node c : parent.children) {
            if (best == null || c.visits > best.visits) best = c;
        }
        if (best == null) throw new IllegalStateException("No more children");
        return best;
    }

    private List<MoveStep> getAllMoveSteps(GameState s) {
        int[] amazons = (s.toMove == Player.WHITE) ? s.whiteAmazons : s.blackAmazons;
        List<MoveStep> moves = new ArrayList<>(200);
        for (int from : amazons) {
            for (int to : MoveGen.genQueenMoves(s.board, from)) {
                moves.add(new MoveStep(from, to));
            }
        }
        return moves;
    }

    // Sample random full moves
    // returns empty list if terminal.
    private List<FullTurn> sampleRandomFullMoves(GameState s, int n) {
        if (s.isTerminal()) return Collections.emptyList();
        List<FullTurn> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(randomFullMove(s));
        }
        return out;
    }

    private FullTurn randomFullMove(GameState s) {
        MoveStep move  = randomMoveStep(s);
        GameState after = s.applyMove(move);
        List<Integer> arrows = MoveGen.genQueenShotsAfterMove(after.board, move.from(), move.to());
        int arrow = arrows.get(rng.nextInt(arrows.size()));
        return new FullTurn(move.from(), move.to(), arrow);
    }

    private MoveStep randomMoveStep(GameState s) {
        List<MoveStep> moves = getAllMoveSteps(s);
        if (moves.isEmpty()) throw new IllegalStateException("Terminal state reached unexpectedly.");
        return moves.get(rng.nextInt(moves.size()));
    }

    private GameState applyFullMove(GameState s, FullTurn m) {
        GameState afterMove = s.applyMove(new MoveStep(m.from(), m.to()));
        return afterMove.shoot(m.to(), new ShootStep(m.arrow()));
    }

    //Removes and returns a uniformly random element
    @SuppressWarnings("unchecked")
    private <T> T removeRandom(List<T> list) {
        int idx = rng.nextInt(list.size());
        T val = list.get(idx);
        list.set(idx, list.get(list.size() - 1));
        list.remove(list.size() - 1);
        return val;
    }

    private static final class Node {
        Node parent = null;
        List<Node> children = new ArrayList<>();
        int visits = 0;
        double wins = 0.0;

        /* 
        Action that led to this node.
        Move tree : MoveStep (null for root).
        Shot tree : Integer arrow square (root) or FullMove (deeper nodes). 
        */
        final Object     action;

        /*Game state after applying action.
         Move tree root : before-move state.
         Shot tree root : after-move state (arrow not yet fired).
         All other nodes : a regular full game state.
        */
        final GameState  state;

        /* Actions not yet expanded from this node. */
        final List<Object> untried;

        @SuppressWarnings("unchecked")
        Node(Object action, GameState state, List<?> untriedActions) {
            this.action = action;
            this.state = state;
            this.untried = new ArrayList<>((List<Object>) untriedActions);
        }
    }
}
