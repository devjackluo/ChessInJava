package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class AlphaBetaThread implements MoveStrategy {

    private final BoardEvaluator boardEvaluator;
    private final int searchDepth;

    public AlphaBetaThread(final int searchDepth) {
        this.boardEvaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
    }

    @Override
    public String toString() {
        return "MiniMax";
    }

    @Override
    public Move execute(Board board) {

//        Move bestMove = null;
//
//        int highestSeenValue = Integer.MIN_VALUE;
//        int lowestSeenValue = Integer.MAX_VALUE;
//
//        int currentValue;

        AtomicReference<Move> bestMove = null;
        AtomicInteger highestSeenValue = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger lowestSeenValue = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger currentValue = new AtomicInteger(Integer.MAX_VALUE);

        System.out.println(board.currentPlayer() + " Thinking with depth = " + this.searchDepth);

        final AtomicInteger depth = new AtomicInteger(this.searchDepth);

        ExecutorService es = Executors.newFixedThreadPool(6);

        for (final Move move : board.currentPlayer().getLegalMoves()) {

            es.submit(

                    new Thread(() -> {

                        final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
                        if (moveTransition.getMoveStatus().isDone()) {

                            //synchronized (currentValue) {

                            currentValue.set(board.currentPlayer().getAlliance().isWhite() ?
                                    min(moveTransition.getTransitionBoard(), depth.get() - 1, highestSeenValue.get(), lowestSeenValue.get()) :
                                    max(moveTransition.getTransitionBoard(), depth.get() - 1, highestSeenValue.get(), lowestSeenValue.get())
                            );

                            //}

                            if (board.currentPlayer().getAlliance().isWhite() && currentValue.get() > highestSeenValue.get()) {
                                highestSeenValue.set(currentValue.get());
                                bestMove.set(move);
                            }

                            if (board.currentPlayer().getAlliance().isBlack() && currentValue.get() < lowestSeenValue.get()) {
                                lowestSeenValue.set(currentValue.get());

                                bestMove.set(move);
                                //bestMove = m;
                            }
                        }

                    })

            );

        }

        try {
            es.shutdown();
            es.awaitTermination(100000, TimeUnit.MINUTES);
        }catch (Exception e){
            System.out.println(e);
        }

        //return the best move
        return bestMove.get();

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
