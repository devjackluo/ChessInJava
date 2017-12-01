package com.chess.engine.player.ai;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.*;
import com.chess.engine.player.Player;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.*;

public class AlphaBetaWithMoveOrdering extends Observable implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private final MoveSorter moveSorter;
    private final int quiescenceFactor;
    private long boardsEvaluated;
    private long executionTime;
    private int quiescenceCount;
    private int cutOffsProduced;

    private enum MoveSorter {

        SORT {
            @Override
            Collection<Move> sort(final Collection<Move> moves) {
                return Ordering.from(SMART_SORT).immutableSortedCopy(moves);
            }
        };

        public static Comparator<Move> SMART_SORT = new Comparator<Move>() {
            @Override
            public int compare(final Move move1, final Move move2) {
                return ComparisonChain.start()
                        .compareTrueFirst(BoardUtils.isThreatenedBoardImmediate(move1.getBoard()), BoardUtils.isThreatenedBoardImmediate(move2.getBoard()))
                        .compareTrueFirst(move1.isAttack(), move2.isAttack())
                        .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                        .compare(move2.getMovedPiece().getPieceValue(), move1.getMovedPiece().getPieceValue())
                        .result();
            }
        };

        abstract Collection<Move> sort(Collection<Move> moves);
    }

    public AlphaBetaWithMoveOrdering(final int searchDepth,
                                     final int quiescenceFactor) {
        this.evaluator = StandardBoardEvaluator.get();
        this.searchDepth = searchDepth;
        this.quiescenceFactor = quiescenceFactor;
        this.moveSorter = MoveSorter.SORT;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
        this.cutOffsProduced = 0;
    }

    @Override
    public String toString() {
        return "AB+MO";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        final Player currentPlayer = board.currentPlayer();
        final Alliance alliance = currentPlayer.getAlliance();
        Move bestMove = Move.MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        int moveCounter = 1;
        final int numMoves = this.moveSorter.sort(board.currentPlayer().getLegalMoves()).size();
        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);
        //System.out.println("\tOrdered moves! : " + this.moveSorter.sort(board.currentPlayer().getLegalMoves()));


        List<Move> actualMoves = new ArrayList<>(35);
        for(Move move : board.currentPlayer().getLegalMoves()){
            final MoveTransition MoveTransition = board.currentPlayer().makeMove(move);
            if(MoveTransition.getMoveStatus().isDone()){

                final boolean makeAnotherMoveTwo = checkKnightFork(board, move);
                final boolean makeAnotherMoveThree = checkQueenEscapes(board, move);
                final boolean makeAnotherMoveSeven = futurePawnAttack(board, move);

                if (makeAnotherMoveSeven == false && makeAnotherMoveTwo == false && makeAnotherMoveThree == false) {
                    actualMoves.add(move);
                }
            }
        }

        if(actualMoves.size() == 0){
            actualMoves = ImmutableList.copyOf(board.currentPlayer().getLegalMoves());
        }



        for (final Move move : this.moveSorter.sort(actualMoves)) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            this.quiescenceCount = 0;
            final String s;
            if (moveTransition.getMoveStatus().isDone()) {
                final long candidateMoveStartTime = System.nanoTime();
                currentValue = alliance.isWhite() ?
                        min(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue) :
                        max(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue);
                if (alliance.isWhite() && currentValue > highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                    //setChanged();
                    //notifyObservers(bestMove);
                }
                else if (alliance.isBlack() && currentValue < lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                    //setChanged();
                    //notifyObservers(bestMove);
                }
                final String quiescenceInfo = " [h: " +highestSeenValue+ " l: " +lowestSeenValue+ "] q: " +this.quiescenceCount;
                s = "\t" + toString() + "(" +this.searchDepth+ "), m: (" +moveCounter+ "/" +numMoves+ ") " + move + ", best:  " + bestMove

                        + quiescenceInfo + ", t: " +calculateTimeTaken(candidateMoveStartTime, System.nanoTime());
            } else {
                s = "\t" + toString() + ", m: (" +moveCounter+ "/" +numMoves+ ") " + move + " is illegal, best: " +bestMove;
            }
            System.out.println(s);
            setChanged();
            notifyObservers(s);
            moveCounter++;
        }
        this.executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("%s SELECTS %s [#boards evaluated = %d, time taken = %d ms, eval rate = %.1f cutoffCount = %d prune percent = %.2f\n", board.currentPlayer(),
                bestMove, this.boardsEvaluated, this.executionTime, (1000 * ((double)this.boardsEvaluated/this.executionTime)), this.cutOffsProduced, 100 * ((double)this.cutOffsProduced/this.boardsEvaluated));
        return bestMove;
    }

    public int max(final Board board,
                   final int depth,
                   final int highest,
                   final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentHighest = highest;

        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentHighest = Math.max(currentHighest, min(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, move, depth), currentHighest, lowest));
                if (lowest <= currentHighest) {
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentHighest;
    }

    public int min(final Board board,
                   final int depth,
                   final int highest,
                   final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentLowest = lowest;

        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentLowest = Math.min(currentLowest, max(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, move, depth), highest, currentLowest));
                if (currentLowest <= highest) {
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentLowest;
    }

    private int calculateQuiescenceDepth(final Board board,
                                         final Move move,
                                         final int depth) {
        return depth - 1;
    }

    private static String calculateTimeTaken(final long start, final long end) {
        final long timeTaken = (end - start) / 1000000;
        return timeTaken + " ms";
    }

























    private boolean checkQueenEscapes(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();

        //if my opponent moves anything and then I move but fail to save queen then the move is bad
        for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {
            final MoveTransition opponentMoveBoard = opponentToMoveBoard.currentPlayer().makeMove(oppMove);
            final Board meToMoveBoard = opponentMoveBoard.getToBoard();
            int queenedAttackedOnBoards = 0;
            for (final Move meMove : meToMoveBoard.currentPlayer().getLegalMoves()) {
                final MoveTransition meMovedBoard = meToMoveBoard.currentPlayer().makeMove(meMove);
                final Board oppToMoveAgainBoard = meMovedBoard.getToBoard();
                boolean queenAttackedEach = false;
                for (Piece piece : oppToMoveAgainBoard.currentPlayer().getActivePieces()) {
                    for (Move move : piece.calculateLegalMoves(oppToMoveAgainBoard)) {
                        if (move.getAttackedPiece() instanceof Queen && !(move.getMovedPiece() instanceof Queen)) {
                            queenAttackedEach = true;
                        }
                    }
                }
                if (queenAttackedEach) {
                    queenedAttackedOnBoards++;
                }
            }
            if (queenedAttackedOnBoards == meToMoveBoard.currentPlayer().getLegalMoves().size() - 1 || queenedAttackedOnBoards == meToMoveBoard.currentPlayer().getLegalMoves().size()) {
                makeOtherMove = true;
            }
        }

        return makeOtherMove;

    }


    public boolean futurePawnAttack(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        //i make my move
        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();
        boolean once = false;


        if (true) {

            //my opponents moves after
            for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {

                //he attacks with a pawn forward
                if (oppMove.getMovedPiece() instanceof Pawn) {

                    final MoveTransition oppMoved = opponentToMoveBoard.currentPlayer().makeMove(oppMove);
                    final Board meToMoveBoard = oppMoved.getToBoard();

                    if(meToMoveBoard.getTile(oppMove.getDestinationCoordinate()).getPiece() != null) {

                        for (Move movess : meToMoveBoard.getTile(oppMove.getDestinationCoordinate()).getPiece().calculateLegalMoves(meToMoveBoard)) {

                            if (movess.getAttackedPiece() instanceof Bishop || movess.getAttackedPiece() instanceof Rook || movess.getAttackedPiece() instanceof Queen || movess.getAttackedPiece() instanceof Knight) {

                                makeOtherMove = true;

                                //i try save the major piece
                                for (final Move meSaveMajor : meToMoveBoard.currentPlayer().getLegalMoves()) {


                                    if (!(meSaveMajor.isMajorAttack()) || (meSaveMajor.isMajorAttack() && meSaveMajor.getMovedPiece().getPieceValue() <= meSaveMajor.getAttackedPiece().getPieceValue())) {
                                        //i make a move to try save piece
                                        final MoveTransition meMoved = meToMoveBoard.currentPlayer().makeMove(meSaveMajor);
                                        final Board oppToAtkMajorBoard = meMoved.getToBoard();
                                        //for all opponent moves again
                                        int cantKill = 0;
                                        for (final Move oppAtkMajor : oppToAtkMajorBoard.currentPlayer().getLegalMoves()) {
                                            //if he can still attack a bishop
                                            if (((oppAtkMajor.getAttackedPiece() instanceof Bishop || oppAtkMajor.getAttackedPiece() instanceof Queen || oppAtkMajor.getAttackedPiece() instanceof Knight || oppAtkMajor.getAttackedPiece() instanceof Rook)&& oppAtkMajor.getMovedPiece() instanceof Pawn)) {
                                                //System.out.println(oppToAtkMajorBoard);
                                                cantKill -= 1;
                                            }
                                            cantKill++;
                                        }
                                        if (cantKill == oppToAtkMajorBoard.currentPlayer().getLegalMoves().size() && once == false) {
                                            once = true;
                                        }
                                    }


                                }


                            }

                        }


                    }

                }
            }

        }


        if(once == true){
            makeOtherMove = false;
        }

        return makeOtherMove;

    }



    private boolean checkKnightFork(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();

        //#########knight fork


        boolean firstYes = false;
        boolean secondYes = false;
        boolean firstNo = false;
        boolean secondNo = false;


        //System.out.println(opponentToMoveBoard.currentPlayer().getAlliance());

        //for all my opponents moves
        for (final Piece oppKnight : opponentToMoveBoard.currentPlayer().getActivePieces()) {
            //for the knight moves
            if (oppKnight instanceof Knight) {
                //get knight
                final Piece enemyKnight = oppKnight;
                //for all the moves of the knight
                for (final Move kMoves : enemyKnight.calculateLegalMoves(opponentToMoveBoard)) {
                    //the knights does the move
                    final MoveTransition oppMovesKnight = opponentToMoveBoard.currentPlayer().makeMove(kMoves);
                    final Board meToAnalyzeKnightBoard = oppMovesKnight.getToBoard();
                    //System.out.println(meToAnalyzeKnightBoard.currentPlayer().getAlliance());
                    Piece myQueen;
                    for (Piece queen : meToAnalyzeKnightBoard.currentPlayer().getActivePieces()) {
                        if (queen instanceof Queen) {
                            myQueen = queen;
                        }
                    }
                    //if that move leaves me in check          //or attacking queen???
                    if (meToAnalyzeKnightBoard.currentPlayer().isInCheck()) {
                        Piece newKnight = meToAnalyzeKnightBoard.getTile(kMoves.getDestinationCoordinate()).getPiece();
                        //for all my moves moves
                        for (Move outOfCheck : meToAnalyzeKnightBoard.currentPlayer().getLegalMoves()) {
                            //then if i were to move out of check
                            final MoveTransition meMovedOutOfCheck = meToAnalyzeKnightBoard.currentPlayer().makeMove(outOfCheck);
                            final Board oppToMoveAfterKingMoveBoard = meMovedOutOfCheck.getToBoard();
                            if (meMovedOutOfCheck.getMoveStatus().isDone()) {
                                //but then my opponent still can attack a queen bishop or rook then it is bad
                                for (final Move oppAny : oppToMoveAfterKingMoveBoard.currentPlayer().getLegalMoves()) {
                                    if (oppAny.getMovedPiece().equals(newKnight)) {
                                        if (oppAny.getAttackedPiece() instanceof Queen || oppAny.getAttackedPiece() instanceof Bishop || oppAny.getAttackedPiece() instanceof Rook) {
                                            //then bad move
                                            firstNo = true;
                                        }
                                    }
                                }
                                if (outOfCheck.getDestinationCoordinate() == kMoves.getDestinationCoordinate()) {
                                    //System.out.println("2############################ FORK BY KNIGHT i can kill it");
                                    firstYes = true;
                                    final MoveTransition meAttackedKnight = meToAnalyzeKnightBoard.currentPlayer().makeMove(outOfCheck);
                                    final Board oppToMoveAfterIKillKnight = meAttackedKnight.getToBoard();
                                    for (Move atkBack : oppToMoveAfterIKillKnight.currentPlayer().getLegalMoves()) {
                                        if (atkBack.getDestinationCoordinate() == kMoves.getDestinationCoordinate()) {
                                            //System.out.println("2############################ FORK BY KNIGHT teamup");
                                            secondNo = true;
                                            final MoveTransition oppKilledMyKilledPiece = oppToMoveAfterIKillKnight.currentPlayer().makeMove(outOfCheck);
                                            final Board meToKillPieceKilledPiece = oppKilledMyKilledPiece.getToBoard();
                                            for (Move killPiece : meToKillPieceKilledPiece.currentPlayer().getLegalMoves()) {
                                                if (killPiece.getDestinationCoordinate() == kMoves.getDestinationCoordinate()) {
                                                    if (outOfCheck.getMovedPiece().getPieceValue() <= atkBack.getMovedPiece().getPieceValue()) {
                                                        secondYes = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (firstNo == true) {
            //System.out.println("2############################ FORK BY KNIGHT");
            makeOtherMove = true;
        }

        if (firstYes == true) {
            //System.out.println("2############################ FORK BY KNIGHT but i kill");
            makeOtherMove = false;
        }

        if (secondNo == true) {
            //System.out.println("2############################ FORK BY KNIGHT teamup");
            makeOtherMove = true;
        }

        if (secondYes == true) {
            makeOtherMove = false;
        }


        return makeOtherMove;

    }






















}