package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class AlphaBetaThreadTwo implements MoveStrategy {

    private final BoardEvaluator boardEvaluator;
    private final int searchDepth;


    private Move bestMove = null;
    private int highestSeenValue = Integer.MIN_VALUE;
    private int lowestSeenValue = Integer.MAX_VALUE;
    private int currentValue;


    public AlphaBetaThreadTwo(final int searchDepth) {
        this.boardEvaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
    }

    @Override
    public String toString() {
        return "MiniMax";
    }

    @Override
    public Move execute(Board board) {


        System.out.println(board.currentPlayer() + " Thinking with depth = " + this.searchDepth);

        ExecutorService es = Executors.newFixedThreadPool(6);

        //loop all moves
        for (final Move move : board.currentPlayer().getLegalMoves()) {

            es.submit(() -> {

                        //for all moves, make that move and get the transition board
                        final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

                        //if the moveTransition confirms the board is a valid move
                        if (moveTransition.getMoveStatus().isDone()) {

                            //then call minimax on that move.


                            currentValue = board.currentPlayer().getAlliance().isWhite() ?
                                    min(moveTransition.getTransitionBoard(), searchDepth - 1, highestSeenValue, lowestSeenValue) :
                                    max(moveTransition.getTransitionBoard(), searchDepth - 1, highestSeenValue, lowestSeenValue);


//                            if (board.currentPlayer().getAlliance().isWhite() && currentValue < holderInt) {
//                                currentValue = holderInt;
//                            }
//
//                            if (board.currentPlayer().getAlliance().isBlack() && currentValue > holderInt) {
//                                currentValue = holderInt;
//                            }


                            //minimax will traverse all the possible moves and record the best scores
                            //in alpha beta, can't be equal because it'll be returning the best back up
                            if (board.currentPlayer().getAlliance().isWhite() && currentValue > highestSeenValue) {
                                highestSeenValue = currentValue;
                                bestMove = move;
                            }

                            if (board.currentPlayer().getAlliance().isBlack() && currentValue < lowestSeenValue) {
                                lowestSeenValue = currentValue;
                                bestMove = move;
                            }


                        }

                    }

            );

        }


//        if (board.currentPlayer().getAlliance().isWhite() ) {
//            System.out.println(highestSeenValue);
//        }
//
//        if (board.currentPlayer().getAlliance().isBlack() ) {
//            System.out.println(lowestSeenValue);
//        }

        try {
            es.shutdown();
            es.awaitTermination(100000, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println(bestMove);

        //return the best move
        return bestMove;

    }

    private int max(final Board board,
                    final int depth,
                    final int highest,
                    final int lowest) {

        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }

        //keep track of alpha
        int currentHighest = highest;

        //traverse tree until leaf
        for (final Move move : (board.currentPlayer().getLegalMoves())) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentHighest = Math.max(currentHighest, min(moveTransition.getTransitionBoard(), depth - 1, currentHighest, lowest));

                //if max find something better than lowest, min will always take the lowest (whatever it was before)
                if (currentHighest >= lowest) {
                    return lowest;
                }
            }
        }
        return currentHighest;
    }

    private int min(final Board board,
                    final int depth,
                    final int highest,
                    final int lowest) {

        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }

        //keep track of beta
        int currentLowest = lowest;

        for (final Move move : (board.currentPlayer().getLegalMoves())) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentLowest = Math.min(currentLowest, max(moveTransition.getTransitionBoard(), depth - 1, highest, currentLowest));

                // if min finds something lower than highest, max will always take highest path (whatever it was better)
                if (currentLowest <= highest) {
                    return highest;
                }
            }
        }
        return currentLowest;
    }


    //TODO check if this is correct board evaluation to return.
    private static boolean isEndGameScenario(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }


}
