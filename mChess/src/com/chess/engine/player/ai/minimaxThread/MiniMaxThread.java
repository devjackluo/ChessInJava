package com.chess.engine.player.ai.minimaxThread;


import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.player.ai.BoardEvaluator;
import com.chess.engine.player.ai.MiniMax;
import com.chess.engine.player.ai.StandardBoardEvaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MiniMaxThread implements Runnable {

    private Thread t;
    private Move BestMove = null;
    private Board TheBoard;
    private final int searchDepth;
    private final BoardEvaluator evaluator;
    private final Collection<Move> ThreadMoves;


    public MiniMaxThread(final Board board, final int searchDepth, final Collection<Move> threadMoves){

        this.TheBoard = board;
        this.searchDepth = searchDepth;
        this.evaluator = StandardBoardEvaluator.get();
        this.ThreadMoves = threadMoves;



    }

    @Override
    public void run() {



        Move bestMove = Move.MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;


        for (final Move move : this.ThreadMoves) {

            final MoveTransition moveTransition = this.TheBoard.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {

                currentValue = this.TheBoard.currentPlayer().getAlliance().isWhite() ?
                        min(moveTransition.getToBoard(), this.searchDepth - 1) :
                        max(moveTransition.getToBoard(), this.searchDepth - 1);

                if (this.TheBoard.currentPlayer().getAlliance().isWhite() &&
                        currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                } else if (this.TheBoard.currentPlayer().getAlliance().isBlack() &&
                        currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                }
            }


        }


        this.BestMove = bestMove;


    }



    public void startMiniMax() throws InterruptedException {



        if (t == null) {

            t = new Thread (this);

            //t.setDaemon(true);

            t.start ();


            //t.join();

        }
    }

    public Move returnBestMove(){
        return this.BestMove;
    }



    public int min(final Board board,
                   final int depth) {
        if(depth == 0) {
            return this.evaluator.evaluate(board, depth);
        }
        if(isEndGameScenario(board)) {
            return this.evaluator.evaluate(board, depth);
        }
        int lowestSeenValue = Integer.MAX_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final int currentValue = max(moveTransition.getToBoard(), depth - 1);
                if (currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                }
            }
        }
        return lowestSeenValue;
    }

    public int max(final Board board,
                   final int depth) {
        if(depth == 0) {
            return this.evaluator.evaluate(board, depth);
        }
        if(isEndGameScenario(board)) {
            return this.evaluator.evaluate(board, depth);
        }
        int highestSeenValue = Integer.MIN_VALUE;
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final int currentValue = min(moveTransition.getToBoard(), depth - 1);
                if (currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                }
            }
        }
        return highestSeenValue;
    }

    private static boolean isEndGameScenario(final Board board) {
        return  board.currentPlayer().isInCheckMate() ||
                board.currentPlayer().isInStaleMate();
    }


}
