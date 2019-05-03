package ArtificialStupidity;

import martijn.quoridor.brains.Brain;
import martijn.quoridor.model.*;

import java.util.*;


public class TestBrain extends Brain {
    //    private ArrayList<Position> pathToGoal(Position start,Orientation[] orientations){
//        ArrayList<Position> path=new ArrayList<>();
//        int x=start.getX();
//        int y=start.getY();
//        for (Orientation or:orientations
//        ) {
//            switch (or){
//                case EAST:
//                    x++;
//                    break;
//                case WEST:
//                    x--;
//                    break;
//                case NORTH:
//                    y++;
//                    break;
//                case SOUTH:
//                    y--;
//                    break;
//            }
//            path.add(new Position(x,y));
//        }
//        return path;
//    }
    private int calculateUtility(State state){
        return (state.oppDistance<3 || state.oppDistance<state.myDistance) && state.wallCount!=0 ? (int)Math.pow(2.00,state.oppDistance) :
                Integer.MAX_VALUE-state.myDistance;
    }
    private ArrayList<SUCCESSOR> successors(State state,Board board){
        Player me=board.getTurn();
        Player opp=board.getPlayers()[1- board.getTurn().getIndex()];
        ArrayList<Move> actions = new ArrayList<>();
        ArrayList<SUCCESSOR> SUCCESSORS=new ArrayList<>();
        State tempState;
        SUCCESSOR successor;
        STATE_MODE stateMode=(state.stateMode == STATE_MODE.MAX) ? STATE_MODE.MIN : STATE_MODE.MAX;
        int depth=state.depth + 1;

        if(state.stateMode==STATE_MODE.MAX) {
            ArrayList<Jump> jumps=new ArrayList<>();
            for (Position pos:me.getJumpPositions())
                jumps.add(new Jump(pos));
            actions.addAll(jumps);
            int oppDistance=opp.findGoal().length;
            int myDistance;
            for (Move move:actions) {
                move.execute(board);
                myDistance=me.findGoal().length;
                tempState = new State(stateMode,depth,myDistance,oppDistance,board.getTurn().getWallCount());
                successor = new SUCCESSOR(tempState,move,calculateUtility(tempState));
                SUCCESSORS.add(successor);
                move.undo(board);
            }
            actions.clear();
        }
        else{
            Position realPos=opp.getPosition();
            int myDistance=me.findGoal().length;
            int oppDistance;
            int wallCount=board.getTurn().getWallCount();
            for (Position pos:opp.getJumpPositions()){
                opp.setPosition(pos);
                oppDistance=opp.findGoal().length;
                tempState=new State(stateMode,depth,myDistance,oppDistance,wallCount);
                SUCCESSORS.add(new SUCCESSOR(tempState,null,calculateUtility(tempState)));
            }
            opp.setPosition(realPos);
        }

        if((state.stateMode==STATE_MODE.MAX && me.getWallCount()>0) ||
                (state.stateMode==STATE_MODE.MIN && opp.getWallCount()>0)){
            PutWall putWall;
            Position tmp;
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    tmp = new Position(x, y);
                    putWall = new PutWall(tmp, Wall.HORIZONTAL);
                    actions.add(putWall);
                    putWall = new PutWall(tmp, Wall.VERTICAL);
                    actions.add(putWall);
                }
            }

            int oppDistance;
            int myDistance;
            for (Move move:actions
            ) {
                if(move.isLegal(board)) {
                    move.execute(board);
                    myDistance=me.findGoal().length;
                    oppDistance=opp.findGoal().length;
                    tempState = new State(stateMode,depth,myDistance,oppDistance,board.getTurn().getWallCount());
                    successor = new SUCCESSOR(tempState,move,calculateUtility(tempState));
                    SUCCESSORS.add(successor);
                    move.undo(board);
                }
            }
        }
        return SUCCESSORS;
    }


    private int min_value(State state,Board board,int alpha,int beta){
        if(state.depth>2) return state.utility;
        int v=Integer.MAX_VALUE;
        ArrayList<SUCCESSOR> successors=successors(state,board);
        for (SUCCESSOR successor:successors){
            v=Integer.min(v,max_value(successor.state,board,alpha,beta));
            if(v<=alpha) return v;
            beta=Integer.min(v,beta);
        }
        return v;
    }
    private int max_value(State state,Board board,int alpha,int beta){
        if(state.depth>2) return state.utility;
        int v=Integer.MIN_VALUE;
        ArrayList<SUCCESSOR> successors=successors(state,board);
        for (SUCCESSOR successor:successors){
            v=Integer.max(v,min_value(successor.state,board,alpha,beta));
            if(v>=beta) return v;
            alpha=Integer.max(v,alpha);
        }
        return v;
    }

    @Override
    public Move getMove(Board board) {

        State state=new State(STATE_MODE.MAX,0,board.getTurn().findGoal().length,
                board.getPlayers()[1-board.getTurn().getIndex()].findGoal().length,
                board.getTurn().getWallCount()
        );
        int v = max_value(state,board, Integer.MIN_VALUE, Integer.MAX_VALUE);
        ArrayList<SUCCESSOR> scc = successors(state,board);
        Move move=null;
        for (SUCCESSOR successor:scc
        ) {
            if(successor.state.utility== v){
                move=successor.action;
                break;
            }
        }
        return move;
    }
}
