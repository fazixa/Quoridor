package ArtificialStupidity;

import martijn.quoridor.brains.Brain;
import martijn.quoridor.model.*;

import java.util.*;

class oppPutWall implements Move {

    private Position position;

    private Wall wall;

    oppPutWall(Position position, Wall wall) {
        if (position == null || wall == null) {
            throw new NullPointerException();
        }
        this.position = position;
        this.wall = wall;
    }

    public Position getPosition() {
        return position;
    }

    public Wall getWall() {
        return wall;
    }

    /**
     * Creates and returns a PutWall move at the same position but with the wall
     * direction flipped.
     */
    public martijn.quoridor.model.PutWall flip() {
        return new martijn.quoridor.model.PutWall(position, wall.flip());
    }

    public void execute(Board board) {
        board.setWall(position, wall);
        board.getPlayers()[1 - board.getTurn().getIndex()].takeWall();
    }

    public void undo(Board board) {
        board.getPlayers()[1 - board.getTurn().getIndex()].giveWall();
        board.setWall(position, null);
    }

    public boolean isLegal(Board board) {
        // Does position exist on board?
        if (!board.containsWallPosition(position)) {
            return false;
        }

        // Is the game over?
        if (board.isGameOver()) {
            return false;
        }

        // Is there already a wall at the position?
        if (board.getWall(position) != null) {
            return false;
        }

        // Does the player have any walls left to place?
        if (board.getPlayers()[1 - board.getTurn().getIndex()].getWallCount() < 1) {
            return false;
        }

        // Does the wall clash with existing walls nearby?
        Position p1, p2;
        switch (wall) {
            case HORIZONTAL:
                p1 = position.west();
                p2 = position.east();
                break;
            case VERTICAL:
                p1 = position.north();
                p2 = position.south();
                break;
            default:
                throw new InternalError();
        }
        if (board.containsWallPosition(p1) && board.getWall(p1) == wall) {
            return false;
        }
        if (board.containsWallPosition(p2) && board.getWall(p2) == wall) {
            return false;
        }

        // Would the wall block a player from reaching their goal?
        try {
            execute(board);
            for (Player p : board.getPlayers()) {
                if (p.findGoal() == null) {
                    // This player's has no way of reaching their goal anymore.
                    return false;
                }
            }
        } finally {
            undo(board);
        }

        // Placing the wall is okay.
        return true;
    }

    public String toString() {
        return "Opponent PutWall " + getWall().toString() + " at " + getPosition();
    }

}

class oppJump implements Move {

    private Position oldPosition;

    private Position newPosition;

    oppJump(Position newPosition) {
        this.newPosition = newPosition;
    }

    /**
     * Returns the player's position after the jump.
     */
    public Position getNewPosition() {
        return newPosition;
    }

    /**
     * Returns the player's position before the jump. Returns null if the move
     * has never been executed yet.
     */
    public Position getOldPosition() {
        return oldPosition;
    }

    public void execute(Board board) {
        Player p = board.getPlayers()[1 - board.getTurn().getIndex()];
        oldPosition = p.getPosition();
        p.setPosition(newPosition);
    }

    public void undo(Board board) {
        board.getPlayers()[1 - board.getTurn().getIndex()].setPosition(oldPosition);
    }

    public boolean isLegal(Board board) {
        return !board.isGameOver()
                && board.getPlayers()[1 - board.getTurn().getIndex()].getJumpPositions().contains(newPosition);
    }

    public String toString() {
        return "Opponent Jump to " + getNewPosition();
    }

}

enum STATE_MODE {MAX, MIN}

class State {
    double utility;
    State parentState;
    Move action;
    STATE_MODE stateMode;
    int myPositionFeature;
    int oppPositionFeature;
    int depth;

    State(STATE_MODE state_mode, Board board,int depth, State parentState, Move action) {
        stateMode = state_mode;
        this.parentState = parentState;
        this.action = action;
        this.depth=depth;
        if(this.depth<3){
            this.utility=(stateMode==STATE_MODE.MAX)?Integer.MIN_VALUE:Integer.MAX_VALUE;
            return;
        }
        Player me = board.getTurn();
        Player opp = board.getPlayers()[1 - board.getTurn().getIndex()];

//        oppDistanceToGoal=opp.findGoal().length;
//        myDistanceToGoal=me.findGoal().length;
        if (me.getOrientation() == Orientation.NORTH) {
            myPositionFeature = 8 - me.getPosition().getY();
//            oppPositionFeature = opp.getPosition().getY();
        } else {
            myPositionFeature = me.getPosition().getY();
//            oppPositionFeature = 8 - opp.getPosition().getY();
        }

        int positionDifferenceFeature = myPositionFeature - oppPositionFeature;
       int myMovesToNextRow = minMovesToNextRow(me, board);
       int oppMovesToNextRow = minMovesToNextRow(opp, board);
//        int move= (action instanceof Jump)? -4 :0;
        this.utility =  (0.6*myPositionFeature + 0.6001*positionDifferenceFeature + 14.45 * myMovesToNextRow + 6.52*oppMovesToNextRow);

    }

    private int minMovesToNextRow(Player player, Board board) {
        Orientation orientation = (player.getOrientation() == Orientation.NORTH) ? Orientation.SOUTH : Orientation.NORTH;
        Position current = player.getPosition();
        int stepsToRight = 0;
        while (board.isBlocked(current,orientation) && !board.isBlocked(current, Orientation.EAST) ) {
            current = current.east();
            stepsToRight++;
        }
        current = player.getPosition();
        int stepsToLeft = 0;
        while (board.isBlocked(current,orientation) && !board.isBlocked(current, Orientation.WEST) ) {
            current = current.west();
            stepsToLeft++;
        }
        return Integer.min(stepsToLeft, stepsToRight);
    }
}

public class StupidBrain extends Brain {
    private ArrayList<State> successors(State state, Board board) {
        Player me = board.getTurn();
        Player opp = board.getPlayers()[1 - board.getTurn().getIndex()];
        ArrayList<State> SUCCESSORS = new ArrayList<>();
        State tempState;
        STATE_MODE stateMode = (state.stateMode == STATE_MODE.MAX) ? STATE_MODE.MIN : STATE_MODE.MAX;
        int depth=state.depth+1;
        if (state.stateMode == STATE_MODE.MAX) {
            ArrayList<Jump> jumps = new ArrayList<>();
            for (Position pos : me.getJumpPositions())
                jumps.add(new Jump(pos));
            for (Jump jump : jumps) {
                jump.execute(board);
                tempState = new State(stateMode, board,depth, state, jump);
                SUCCESSORS.add(tempState);
                jump.undo(board);
            }
            if (me.getWallCount() > 0) {
                ArrayList<PutWall> putWalls = new ArrayList<>();
                int y = opp.getPosition().getY();
                for (int x = opp.getPosition().getX(); x < board.getWidth(); x++) {
                    myAddWall(board, putWalls, x, y);
                }
                for (int x = opp.getPosition().getX() - 1; x >= 0; x--) {
                    myAddWall(board, putWalls, x, y);
                }
                putWalls.removeIf(wall -> !wall.isLegal(board));
                for (PutWall move : putWalls
                ) {
                    move.execute(board);
                    tempState = new State(stateMode, board,depth, state, move);
                    SUCCESSORS.add(tempState);
                    move.undo(board);
                }
            }
        } else {
            ArrayList<oppJump> oppJumps = new ArrayList<>();
            for (Position pos : opp.getJumpPositions())
                oppJumps.add(new oppJump(pos));
            for (oppJump oppJump : oppJumps) {
                oppJump.execute(board);
                tempState = new State(stateMode, board,depth, state, oppJump);
                SUCCESSORS.add(tempState);
                oppJump.undo(board);
            }
            if (opp.getWallCount() > 0) {
                ArrayList<oppPutWall> oppPutWalls = new ArrayList<>();
                int y = me.getPosition().getY();
                for (int x = me.getPosition().getX(); x < board.getWidth(); x++) {
                    oppAddWall(board, oppPutWalls, x, y);
                }
                for (int x = me.getPosition().getX() - 1; x >= 0; x--) {
                    oppAddWall(board, oppPutWalls, x, y);
                }
                oppPutWalls.removeIf(wall -> !wall.isLegal(board));
                for (oppPutWall putWall : oppPutWalls) {
                    putWall.execute(board);
                    tempState = new State(stateMode, board,depth, state, putWall);
                    SUCCESSORS.add(tempState);
                    putWall.undo(board);
                }
            }
        }
        return SUCCESSORS;
    }

    private void myAddWall(Board board, ArrayList<PutWall> putWalls, int oppX, int oppY) {
        Position tmp;
        PutWall putWall;
        for (int y = oppY; y < board.getHeight(); y++) {
            tmp = new Position(oppX, y);
            putWall = new PutWall(tmp, Wall.HORIZONTAL);
            putWalls.add(putWall);
            putWall = new PutWall(tmp, Wall.VERTICAL);
            putWalls.add(putWall);
        }
        for (int y = oppY - 1; y >= 0; y--) {
            tmp = new Position(oppX, y);
            putWall = new PutWall(tmp, Wall.HORIZONTAL);
            putWalls.add(putWall);
            putWall = new PutWall(tmp, Wall.VERTICAL);
            putWalls.add(putWall);
        }
    }

    private void oppAddWall(Board board, ArrayList<oppPutWall> oppPutWalls, int myX, int myY) {
        Position tmp;
        oppPutWall oppPutWall;
        for (int y = myY; y < board.getHeight(); y++) {
            tmp = new Position(myX, y);
            oppPutWall = new oppPutWall(tmp, Wall.HORIZONTAL);
            oppPutWalls.add(oppPutWall);
            oppPutWall = new oppPutWall(tmp, Wall.VERTICAL);
            oppPutWalls.add(oppPutWall);
        }
        for (int y = myY - 1; y >= 0; y--) {
            tmp = new Position(myX, y);
            oppPutWall = new oppPutWall(tmp, Wall.HORIZONTAL);
            oppPutWalls.add(oppPutWall);
            oppPutWall = new oppPutWall(tmp, Wall.VERTICAL);
            oppPutWalls.add(oppPutWall);
        }
    }

    Move bestMove;

    private void doStateActions(State state, Board board) {
        State parentState = state;
        Stack<Move> actions = new Stack<>();
        while (parentState.action != null) {
            actions.push(parentState.action);
            parentState = parentState.parentState;
        }
        Move tmp;
        while (!actions.empty()) {
            tmp = actions.pop();
            tmp.execute(board);
        }
    }

    private void undoStateActions(State state, Board board) {
        State parentState = state;
        ArrayList<Move> actions = new ArrayList<>();
        while (parentState.action != null) {
            actions.add(parentState.action);
            parentState = parentState.parentState;
        }
        for (Move action : actions
        ) {
            action.undo(board);
        }
    }

//    private int min_value(State state, Board board, int depth, int alpha, int beta) {
//        if (depth == 3) return state.utility;
//        int v = Integer.MAX_VALUE;
//        doStateActions(state, board);
//        ArrayList<State> successors = successors(state, board);
//        undoStateActions(state, board);
//        for (State successor : successors) {
//            v = Integer.min(v, max_value(successor, board, depth + 1, alpha, beta, null));
//            if (v <= alpha) return v;
//            beta = Integer.min(v, beta);
//
//        }
//        return v;
//    }

    private double alphaBetaMinimax(State state, Board board, double alpha, double beta, ArrayList<State> SUCCESSORS) {
        if (state.depth == 3) return state.utility;
        int value;
        double tmpAlpha;
        doStateActions(state, board);
        ArrayList<State> successors = (SUCCESSORS == null) ? successors(state, board) : SUCCESSORS;
        undoStateActions(state, board);

        if (state.stateMode == STATE_MODE.MAX) {
//            value = Integer.MIN_VALUE;
//            state.utility=alpha;
            for (State successor : successors) {
                tmpAlpha = state.utility;
                state.utility = Double.max(state.utility, alphaBetaMinimax(successor, board, state.utility, beta, null));
                if (state.depth==0 && tmpAlpha != state.utility)
                    bestMove = successor.action;
                if (state.utility >= beta)
                    return state.utility;
            }
            return state.utility;
        } else {
//            value = Integer.MAX_VALUE;
//            state.utility=beta;
            for (State successor : successors) {
                state.utility = Double.min(state.utility, alphaBetaMinimax(successor, board, alpha, state.utility, null));
                if (alpha >= state.utility)
                    return state.utility;
            }
            return state.utility;
        }
    }

    @Override
    public Move getMove(Board board) {
        State state = new State(STATE_MODE.MAX, board,0, null, null);
        ArrayList<State> scc = successors(state, board);
//        int v =
        alphaBetaMinimax(state, board,  Integer.MIN_VALUE, Integer.MAX_VALUE, scc);
//        Move move = null;
//        for (State successor : scc
//        ) {
//            if (successor.utility == v) {
//                move = successor.action;
//                break;
//            }
//        }
        return bestMove;
    }
}
