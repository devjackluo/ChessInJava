package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;


/**
 *
 * Minimax constructor creates a board evaulator
 *
 * Execute is called
 *
 * */

public class MiniMax implements MoveStrategy {

    private final BoardEvaluator boardEvaluator;
    private final int searchDepth;

    public MiniMax(final int searchDepth) {
        this.boardEvaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
    }

    @Override
    public String toString() {
        return "MiniMax";
    }

    @Override
    public Move execute(Board board) {

        //Holder best move.
        Move bestMove = null;

        //Holder for minimax values seen
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;

        //holder for current value
        int currentValue;

        System.out.println(board.currentPlayer() + " Thinking with depth = " + this.searchDepth);

        //loop all moves
        for (final Move move : board.currentPlayer().getLegalMoves()) {

            //for all moves, make that move and get the transition board
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

            //if the moveTransition confirms the board is a valid move
            if (moveTransition.getMoveStatus().isDone()) {

                //then call minimax on that move.
                currentValue = board.currentPlayer().getAlliance().isWhite()
                        ? min(moveTransition.getTransitionBoard(), this.searchDepth - 1)
                        : max(moveTransition.getTransitionBoard(), this.searchDepth - 1);

                //minimax will traverse all the possible moves and record the best scores
                if (board.currentPlayer().getAlliance().isWhite() && currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                }

                if (board.currentPlayer().getAlliance().isBlack() && currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                }

            }
        }


        if (board.currentPlayer().getAlliance().isWhite() ) {
            System.out.println(highestSeenValue);
        }

        if (board.currentPlayer().getAlliance().isBlack() ) {
            System.out.println(lowestSeenValue);
        }

        //return the best move
        return bestMove;

    }


    public int min(final Board board, final int depth) {

        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }

        int lowestSeenValue = Integer.MAX_VALUE;

        //traverse minimax tree until depth is reached.
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final int currentValue = max(moveTransition.getTransitionBoard(), depth - 1);
                if (currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                }
            }
        }

        //if min will return lowest
        return lowestSeenValue;
    }


    public int max(final Board board, final int depth) {


        if (depth == 0 || isEndGameScenario(board)) {
            return this.boardEvaluator.evaluate(board, depth);
        }

        int highestSeenValue = Integer.MIN_VALUE;

        //traverse minimax tree until depth is reached.
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final int currentValue = min(moveTransition.getTransitionBoard(), depth - 1);
                if (currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                }
            }
        }

        //max will return highest
        return highestSeenValue;
    }


    //TODO check if this is correct board evaluation to return.
    private static boolean isEndGameScenario(final Board board) {
        return board.currentPlayer().isInCheckMate() || board.currentPlayer().isInStaleMate();
    }



}
