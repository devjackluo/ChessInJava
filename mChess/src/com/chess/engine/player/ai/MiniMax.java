package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.player.ai.minimaxThread.MiniMaxThread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class MiniMax implements MoveStrategy{

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private long boardsEvaluated;
    private long executionTime;
    private FreqTableRow[] freqTable;
    private int freqTableIndex;



    public MiniMax(final int searchDepth) {
        this.evaluator = StandardBoardEvaluator.get();
        this.boardsEvaluated = 0;
        this.searchDepth = searchDepth;
    }

    @Override
    public String toString() {
        return "MiniMax";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    public Move execute(final Board board) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        Move bestMove = Move.MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        System.out.println(board.currentPlayer() + " THINKING with depth = " +this.searchDepth);
        this.freqTable = new FreqTableRow[board.currentPlayer().getLegalMoves().size()];
        this.freqTableIndex = 0;
        int moveCounter = 1;
        final int numMoves = board.currentPlayer().getLegalMoves().size();



        Collection<Move> theMoves = board.currentPlayer().getLegalMoves();
        Collection<Move> t1Moves = new ArrayList<>();
        Collection<Move> t2Moves = new ArrayList<>();
        Collection<Move> t3Moves = new ArrayList<>();
        Collection<Move> t4Moves = new ArrayList<>();
        Collection<Move> t5Moves = new ArrayList<>();
        Collection<Move> t6Moves = new ArrayList<>();


        int numThread = 1;
        for(final Move move : theMoves){
            if(numThread == 1){
                t1Moves.add(move);
            }else if (numThread == 2){
                t2Moves.add(move);
            }else if (numThread == 3){
                t3Moves.add(move);
            }else if (numThread == 4){
                t4Moves.add(move);
            }else if (numThread == 5){
                t5Moves.add(move);
            }else if (numThread == 6){
                t6Moves.add(move);
            }
            numThread += 1;
            if(numThread == 7){
                numThread = 1;
            }
        }



        MiniMaxThread mmt1 = new MiniMaxThread(board, this.searchDepth, t1Moves);
        mmt1.startMiniMax();
        Move bestt1Move = mmt1.returnBestMove();

        MiniMaxThread mmt2 = new MiniMaxThread(board, this.searchDepth, t2Moves);
        mmt2.startMiniMax();
        Move bestt2Move = mmt2.returnBestMove();

        MiniMaxThread mmt3 = new MiniMaxThread(board, this.searchDepth, t3Moves);
        mmt3.startMiniMax();
        Move bestt3Move = mmt3.returnBestMove();

        MiniMaxThread mmt4 = new MiniMaxThread(board, this.searchDepth, t4Moves);
        mmt4.startMiniMax();
        Move bestt4Move = mmt4.returnBestMove();

        MiniMaxThread mmt5 = new MiniMaxThread(board, this.searchDepth, t5Moves);
        mmt5.startMiniMax();
        Move bestt5Move = mmt5.returnBestMove();

        MiniMaxThread mmt6 = new MiniMaxThread(board, this.searchDepth, t6Moves);
        mmt6.startMiniMax();
        Move bestt6Move = mmt6.returnBestMove();

        /*
        for (final Move move : board.currentPlayer().getLegalMoves()) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                final FreqTableRow row = new FreqTableRow(move);
                this.freqTable[this.freqTableIndex] = row;
                currentValue = board.currentPlayer().getAlliance().isWhite() ?
                                min(moveTransition.getToBoard(), this.searchDepth - 1) :
                                max(moveTransition.getToBoard(), this.searchDepth - 1);
                System.out.println("\t" + toString() + " analyzing move (" +moveCounter + "/" +numMoves+ ") " + move +
                                   " scores " + currentValue + " " +this.freqTable[this.freqTableIndex]);
                this.freqTableIndex++;
                if (board.currentPlayer().getAlliance().isWhite() &&
                        currentValue >= highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                } else if (board.currentPlayer().getAlliance().isBlack() &&
                        currentValue <= lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                }
            } else {
                System.out.println("\t" + toString() + " can't execute move (" +moveCounter+ "/" +numMoves+ ") " + move);
            }
            moveCounter++;

        }

        this.executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("%s SELECTS %s [#boards = %d time taken = %d ms, rate = %.1f\n", board.currentPlayer(),
                bestMove, this.boardsEvaluated, this.executionTime, (1000 * ((double)this.boardsEvaluated/this.executionTime)));
        long total = 0;
        for (final FreqTableRow row : this.freqTable) {
            if(row != null) {
                total += row.getCount();
            }
        }
        if(this.boardsEvaluated != total) {
            System.out.println("somethings wrong with the # of boards evaluated!");
        }
        */

        while (bestt1Move == null){
            bestt1Move = mmt1.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }

        while (bestt2Move == null){
            bestt2Move = mmt2.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }


        while (bestt3Move == null){
            bestt3Move = mmt3.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }

        while (bestt4Move == null){
            bestt4Move = mmt4.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }

        while (bestt5Move == null){
            bestt5Move = mmt5.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }


        while (bestt6Move == null){
            bestt6Move = mmt6.returnBestMove();
            // prints so it doesn't skip check...
            Thread.sleep(100);
        }


        Collection<Move> threadMoves = new ArrayList<>();
        threadMoves.add(bestt1Move);
        threadMoves.add(bestt2Move);
        threadMoves.add(bestt3Move);
        threadMoves.add(bestt4Move);
        threadMoves.add(bestt5Move);
        threadMoves.add(bestt6Move);

        System.out.println(threadMoves);

        MiniMaxThread allMMT = new MiniMaxThread(board, this.searchDepth, threadMoves);
        allMMT.startMiniMax();
        bestMove = allMMT.returnBestMove();

        while (bestMove == null){
            bestMove = allMMT.returnBestMove();

            // prints so it doesn't skip check...
            Thread.sleep(100);
        }

        return bestMove;

    }

    public int min(final Board board,
                   final int depth) {
        if(depth == 0) {
            this.boardsEvaluated++;
            this.freqTable[this.freqTableIndex].increment();
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
            this.boardsEvaluated++;
            this.freqTable[this.freqTableIndex].increment();
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

    private static class FreqTableRow {

        private final Move move;
        private final AtomicLong count;

        FreqTableRow(final Move move) {
            this.count = new AtomicLong();
            this.move = move;
        }

        public long getCount() {
            return this.count.get();
        }

        public void increment() {
            this.count.incrementAndGet();
        }

        @Override
        public String toString() {
            return BoardUtils.INSTANCE.getPositionAtCoordinate(this.move.getCurrentCoordinate()) +
                   BoardUtils.INSTANCE.getPositionAtCoordinate(this.move.getDestinationCoordinate()) + " : " +this.count;
        }
    }

}
