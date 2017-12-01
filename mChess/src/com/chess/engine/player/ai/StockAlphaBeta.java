package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.*;
import com.chess.engine.player.Player;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.*;

import static com.chess.engine.board.BoardUtils.mvvlva;

public class StockAlphaBeta extends Observable implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private long boardsEvaluated;
    private long executionTime;
    private int quiescenceCount;
    private static final int MAX_QUIESCENCE = 5000;

    private enum MoveSorter {

        STANDARD {
            @Override
            Collection<Move> sort(final Collection<Move> moves) {
                return Ordering.from(new Comparator<Move>() {
                    @Override
                    public int compare(final Move move1,
                                       final Move move2) {
                        return ComparisonChain.start()
                                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                                .compare(mvvlva(move2), mvvlva(move1))
                                .result();
                    }
                }).immutableSortedCopy(moves);
            }
        },
        EXPENSIVE {
            @Override
            Collection<Move> sort(final Collection<Move> moves) {
                return Ordering.from(new Comparator<Move>() {
                    @Override
                    public int compare(final Move move1,
                                       final Move move2) {
                        return ComparisonChain.start()
                                .compareTrueFirst(BoardUtils.kingThreat(move1), BoardUtils.kingThreat(move2))
                                .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                                .compare(mvvlva(move2), mvvlva(move1))
                                .result();
                    }
                }).immutableSortedCopy(moves);
            }
        };

        abstract  Collection<Move> sort(Collection<Move> moves);
    }


    public StockAlphaBeta(final int searchDepth) {
        this.evaluator = StandardBoardEvaluator.get();
        this.searchDepth = searchDepth;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
    }

    @Override
    public String toString() {
        return "StockAlphaBeta";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        final Player currentPlayer = board.currentPlayer();
        Move bestMove = Move.MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);
        int moveCounter = 1;
        int numMoves = board.currentPlayer().getLegalMoves().size();


        List<Move> actualMoves = new ArrayList<>(35);
        for(Move move : board.currentPlayer().getLegalMoves()){
            final MoveTransition MoveTransition = board.currentPlayer().makeMove(move);
            if(MoveTransition.getMoveStatus().isDone()){

                //final boolean makeAnotherMove = checkIfMoveIsGood(board, move);
                final boolean makeAnotherMoveTwo = checkKnightFork(board, move);
                //final boolean makeAnotherMoveThree = checkQueenEscapes(board, move);
                //final boolean makeAnotherMoveFour = checkIfFreeKill(board, move);
                //final boolean makeAnotherMoveFive = pawnIsAttackingMajor(board, move);
                //final boolean makeAnotherMoveSix = majorMovedToDeath(board, move);
                final boolean makeAnotherMoveSeven = futurePawnAttack(board, move);

                if (makeAnotherMoveSeven == false && makeAnotherMoveTwo == false) {
                    actualMoves.add(move);
                }
            }
        }

        if(actualMoves.size() == 0){
            actualMoves = ImmutableList.copyOf(board.currentPlayer().getLegalMoves());
        }


        for (final Move move : MoveSorter.EXPENSIVE.sort((actualMoves))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            this.quiescenceCount = 0;
            final String s;
            if (moveTransition.getMoveStatus().isDone()) {
                final long candidateMoveStartTime = System.nanoTime();
                currentValue = currentPlayer.getAlliance().isWhite() ?
                        min(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue) :
                        max(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue);
                if (currentPlayer.getAlliance().isWhite() && currentValue > highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                    if(moveTransition.getToBoard().blackPlayer().isInCheckMate()) {
                        break;
                    }
                }
                else if (currentPlayer.getAlliance().isBlack() && currentValue < lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                    if(moveTransition.getToBoard().whitePlayer().isInCheckMate()) {
                        break;
                    }
                }

                final String quiescenceInfo = " " + score(currentPlayer, highestSeenValue, lowestSeenValue) + " q: " +this.quiescenceCount;
                s = "\t" + toString() + "(" +this.searchDepth+ "), m: (" +moveCounter+ "/" +numMoves+ ") " + move + ", best:  " + bestMove

                        + quiescenceInfo + ", t: " +calculateTimeTaken(candidateMoveStartTime, System.nanoTime());
            } else {
                s = "\t" + toString() + ", m: (" +moveCounter+ "/" +numMoves+ ") " + move + " is illegal! best: " +bestMove;
                System.out.println("##########################Never Reach Here########################");

            }
            System.out.println(s);
            setChanged();
            notifyObservers(s);
            moveCounter++;
        }

        this.executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("%s SELECTS %s [#boards evaluated = %d, time taken = %d ms, rate = %.1f\n", board.currentPlayer(),
                bestMove, this.boardsEvaluated, this.executionTime, (1000 * ((double)this.boardsEvaluated/this.executionTime)));
        return bestMove;
    }

    private static String score(final Player currentPlayer,
                                final int highestSeenValue,
                                final int lowestSeenValue) {

        if(currentPlayer.getAlliance().isWhite()) {
            return "[score: " +highestSeenValue + "]";
        } else if(currentPlayer.getAlliance().isBlack()) {
            return "[score: " +lowestSeenValue+ "]";
        }
        throw new RuntimeException("bad bad boy!");
    }

    private int max(final Board board,
                    final int depth,
                    final int highest,
                    final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentHighest = highest;
        for (final Move move : MoveSorter.STANDARD.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentHighest = Math.max(currentHighest, min(moveTransition.getToBoard(), calculateQuiescenceDepth(moveTransition, depth), currentHighest, lowest));
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
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentLowest = lowest;
        for (final Move move : MoveSorter.STANDARD.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentLowest = Math.min(currentLowest, max(moveTransition.getToBoard(), calculateQuiescenceDepth(moveTransition, depth), highest, currentLowest));
                if (currentLowest <= highest) {
                    return highest;
                }
            }
        }
        return currentLowest;
    }

    private int calculateQuiescenceDepth(final MoveTransition moveTransition,
                                         final int depth) {
        if(depth == 1 && this.quiescenceCount < MAX_QUIESCENCE) {
            int activityMeasure = 0;
            if (moveTransition.getToBoard().currentPlayer().isInCheck()) {
                activityMeasure += 2;
            }
            for(final Move move: BoardUtils.lastNMoves(moveTransition.getToBoard(), 4)) {
                if(move.isAttack()) {
                    activityMeasure += 1;
                }
            }
            if(activityMeasure > 3) {
                this.quiescenceCount++;
                return 2;
            }
        }
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


    private boolean majorMovedToDeath(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();


        if (bestMove.isMajorMove()) {

            Piece movedMajor = opponentToMoveBoard.getTile(bestMove.getDestinationCoordinate()).getPiece();

            //opponent makes any move
            for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {

                if (oppMove.getDestinationCoordinate() != bestMove.getDestinationCoordinate()) {

                    final MoveTransition oppMoved = opponentToMoveBoard.currentPlayer().makeMove(oppMove);
                    final Board meToSaveMajor = oppMoved.getToBoard();

                    int theSavedMajorLegalMoves = 0;
                    int majorDeadThisMove = 0;
                    for (Move saveMajor : meToSaveMajor.currentPlayer().getLegalMoves()) {

                        //and then i try to move that major
                        if (saveMajor.getMovedPiece().equals(movedMajor)) {

                            theSavedMajorLegalMoves = saveMajor.getMovedPiece().calculateLegalMoves(meToSaveMajor).size();
                            Piece theSavedMajor = saveMajor.getMovedPiece();

                            final MoveTransition iTriedSaveMajor = meToSaveMajor.currentPlayer().makeMove(saveMajor);
                            final Board oppTryKillMajor = iTriedSaveMajor.getToBoard();

                            boolean failToSave = false;
                            //then my opponent's turn again
                            for (Move killMajor : oppTryKillMajor.currentPlayer().getLegalMoves()) {
                                if (killMajor.getDestinationCoordinate() == theSavedMajor.getPiecePosition()) {
                                    failToSave = true;
                                }
                            }
                            if (failToSave) {
                                majorDeadThisMove++;
                            }
                        }
                    }
                    if (theSavedMajorLegalMoves == majorDeadThisMove) {
                        makeOtherMove = true;
                    }
                }
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


    private boolean pawnIsAttackingMajor(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();


        if (!(bestMove.isMajorAttack())) {

            for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {

                if (oppMove.getMovedPiece() instanceof Pawn
                        && (oppMove.getAttackedPiece() instanceof Bishop || oppMove.getAttackedPiece() instanceof Knight
                        || oppMove.getAttackedPiece() instanceof Queen || oppMove.getAttackedPiece() instanceof Rook)) {

                    makeOtherMove = true;

                }
            }
        }

        return makeOtherMove;
    }


    private boolean checkIfFreeKill(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();

        if (bestMove.isMajorMove()) {

            //if i move any major piece
            if (bestMove.getMovedPiece() instanceof Bishop || bestMove.getMovedPiece() instanceof Knight || bestMove.getMovedPiece() instanceof Queen || bestMove.getMovedPiece() instanceof Rook) {

                boolean iCanAttack = false;

                int countAtks = 0;
                //for all my opponents moves
                for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {
                    //if he can attack it
                    if (bestMove.getDestinationCoordinate() == oppMove.getDestinationCoordinate()) {
                        countAtks++;
                        final MoveTransition moveTransition = opponentToMoveBoard.currentPlayer().makeMove(oppMove);
                        final Board meToMove = moveTransition.getToBoard();
                        //then for all my moves
                        for (Move meMove : meToMove.currentPlayer().getLegalMoves()) {

                            //if i can attack back, it is is good
                            if (meMove.getDestinationCoordinate() == oppMove.getDestinationCoordinate()) {
                                iCanAttack = true;
                            }

                        }
                    }

                }

                if (countAtks == 0) {
                    iCanAttack = true;
                }

                if (iCanAttack == false) {
                    makeOtherMove = true;
                }
            }

        }

        return makeOtherMove;

    }


    private boolean checkIfMoveIsGood(Board currentBoard, Move bestMove) {

        boolean makeOtherMove = false;

        final MoveTransition bestMoveBoard = currentBoard.currentPlayer().makeMove(bestMove);
        final Board opponentToMoveBoard = bestMoveBoard.getToBoard();


        boolean goodMoveOne = false;
        boolean goodMoveTwo = false;
        boolean goodMoveThree = false;
        boolean goodMoveFour = false;

        boolean badMoveOne = false;
        boolean badMoveTwo = false;
        boolean badMoveThree = false;
        boolean badMoveFour = false;


        //if I'm attacking anything with lesser value
        if ((bestMove.isMajorAttack()) && bestMove.getAttackedPiece().getPieceValue() < bestMove.getMovedPiece().getPieceValue()) {

            //for all moves really

            //intial ok
            goodMoveOne = true;

            //for all opponent moves
            for (final Move oppMove : opponentToMoveBoard.currentPlayer().getLegalMoves()) {

                //if it attacks the spot i just attacked
                if (oppMove.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                    //then it is bad
                    badMoveOne = true;

                    //if he makes these move.
                    final MoveTransition opponentMovedBoard = opponentToMoveBoard.currentPlayer().makeMove(oppMove);
                    final Board meToMoveBoard = opponentMovedBoard.getToBoard();

                    //then my turn for all my moves.
                    for (final Move meMove : meToMoveBoard.currentPlayer().getLegalMoves()) {

                        //if i can attack back.
                        if (meMove.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                            //and i didn't attack with a queen first then its ok
                            if ((bestMove.getMovedPiece().getPieceValue() <= oppMove.getMovedPiece().getPieceValue())) {
                                goodMoveTwo = true;
                            }

                            //if i were to make this attack back
                            final MoveTransition meMovedBoard = meToMoveBoard.currentPlayer().makeMove(meMove);
                            final Board oppToMoveAgainBoard = meMovedBoard.getToBoard();


                            //then its my opponent's turn and then his moves
                            for (final Move oppMoveAgain : oppToMoveAgainBoard.currentPlayer().getLegalMoves()) {


                                //System.out.println(bestMove.getDestinationCoordinate());
                                //if my opponent can once again attack back
                                if (oppMoveAgain.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                                    //System.out.println(bestMove.getDestinationCoordinate());

                                    //then its bad again
                                    badMoveTwo = true;

                                    //then if he was to make that turn.
                                    final MoveTransition opponentMovedAgainBoard = oppToMoveAgainBoard.currentPlayer().makeMove(oppMoveAgain);
                                    final Board meToMoveAgainBoard = opponentMovedAgainBoard.getToBoard();

                                    //then it is my turn again.
                                    for (final Move meMoveAgain : meToMoveAgainBoard.currentPlayer().getLegalMoves()) {

                                        //if i can attack again.
                                        if (meMoveAgain.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                                            //and i didn't
                                            if ((bestMove.getMovedPiece().getPieceValue() <= oppMove.getMovedPiece().getPieceValue())
                                                    && (meMove.getMovedPiece().getPieceValue() <= oppMoveAgain.getMovedPiece().getPieceValue())) {
                                                goodMoveThree = true;
                                            }

                                            //if i were to make this attack back
                                            final MoveTransition meMovedAgainBoard = meToMoveAgainBoard.currentPlayer().makeMove(meMoveAgain);
                                            final Board oppToMoveAgainAgainBoard = meMovedAgainBoard.getToBoard();

                                            for (Move oppMoveAgainAgain : oppToMoveAgainAgainBoard.currentPlayer().getLegalMoves()) {

                                                if (oppMoveAgainAgain.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                                                    badMoveThree = true;

                                                    final MoveTransition opponentMovedAgainAgainBoard = oppToMoveAgainAgainBoard.currentPlayer().makeMove(oppMoveAgainAgain);
                                                    final Board meToMoveAgainAgainBoard = opponentMovedAgainAgainBoard.getToBoard();

                                                    for (Move meMoveAgainAgain : meToMoveAgainAgainBoard.currentPlayer().getLegalMoves()) {

                                                        if (meMoveAgainAgain.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {


                                                            if ((bestMove.getMovedPiece().getPieceValue() <= oppMove.getMovedPiece().getPieceValue())
                                                                    && (meMove.getMovedPiece().getPieceValue() <= oppMoveAgain.getMovedPiece().getPieceValue())
                                                                    && (meMoveAgain.getMovedPiece().getPieceValue() <= oppMoveAgainAgain.getMovedPiece().getPieceValue())) {
                                                                goodMoveFour = true;
                                                            }


                                                            final MoveTransition meMovedAgainAgainBoard = meToMoveAgainAgainBoard.currentPlayer().makeMove(meMoveAgainAgain);
                                                            final Board oppToMoveAgainAgainAgainBoard = meMovedAgainAgainBoard.getToBoard();


                                                            for (Move oppMoveAgainAgainAgain : oppToMoveAgainAgainAgainBoard.currentPlayer().getLegalMoves()) {

                                                                if (oppMoveAgainAgainAgain.getDestinationCoordinate() == bestMove.getDestinationCoordinate()) {

                                                                    badMoveFour = true;

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

                }
            }

            if (goodMoveOne == true) {
                //System.out.println("Ok move");
                makeOtherMove = false;
            }

            if (badMoveOne == true) {
                //System.out.println("opponent can atk me");
                makeOtherMove = true;
            }

            if (goodMoveTwo == true) {
                //System.out.println("I can attack back and i don't lose value");
                makeOtherMove = false;
            }

            if (badMoveTwo == true) {
                //System.out.println("he atks me again");
                makeOtherMove = true;
            }

            if (goodMoveThree == true) {
                //System.out.println("I attack back again and i don't lose value");
                makeOtherMove = false;
            }

            if (badMoveThree == true) {
                //System.out.println("he attacks me again wtf?");
                makeOtherMove = true;
            }


            if (goodMoveFour == true) {
                //System.out.println("but it is ok, i can attack right back");
                makeOtherMove = false;
            }

            if (badMoveFour == true) {
                //System.out.println("Ok, i give up, his moves are too good");
                makeOtherMove = true;
            }

        }


        //false means its was ok to make this move, true means try something else.
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