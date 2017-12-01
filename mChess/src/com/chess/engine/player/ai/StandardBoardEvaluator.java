package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.pieces.*;
import com.chess.engine.player.Player;
import com.chess.engine.player.ai.KingSafetyAnalyzer.KingDistance;
import com.chess.gui.Table;
import com.google.common.annotations.VisibleForTesting;

import java.util.*;

public final class StandardBoardEvaluator
        implements BoardEvaluator {


    private static final int DONT_DIE_TO_PAWN = -15;
    private static final int MOVE_MAJOR_FIRST = -4;
    private static final int DOUBLE_PAWN_ATTACK_NEGATIVE = -20;
    private static final int QUEENS_DO_NOT_DIE = -30;

    private static final int CASTLED_BONUS = 20;
    private static final int BISHOP_DUO_BONUS = 12;
    private static final int PAWN_IN_CENTER_BONUS = 2;
    private static final int MAJOR_IN_PLAY_BONUS = 3;
    private static final int KING_ALLIES_BONUS = 10;
    private static final int TRADE_MAJOR_BONUS = 6;
    private static final int KILL_THAT_MAJOR_BONUS = 50;
    private static final int UPTRADE_BONUS = 20;
    private static final int NOT_GOOD_TRADE = -20;
    private static final int KING_STILL_FIRST = 40;





    private final static int CHECK_MATE_BONUS = 10000;
    private final static int CHECK_BONUS = 10;
    private final static int CASTLE_BONUS = 25;
    private final static int CASTLE_CAPABLE_BONUS = 10;
    private final static int MOBILITY_MULTIPLIER = 1;
    private final static int ATTACK_MULTIPLIER = 1;
    private final static int TWO_BISHOPS_BONUS = 49;
    private static final StandardBoardEvaluator INSTANCE = new StandardBoardEvaluator();

    private StandardBoardEvaluator() {
    }

    public static StandardBoardEvaluator get() {
        return INSTANCE;
    }

    @Override
    public int evaluate(final Board board,
                        final int depth) {
        return score(board.whitePlayer(), depth, board) - score(board.blackPlayer(), depth, board);
    }

    @VisibleForTesting
    private static int score(final Player player,
                             final int depth, final Board board) {
        return mobility(player) +
               kingThreats(player, depth) +
               attacks(player) +
               castle(player) +
               pieceEvaluations(player) +
               pawnStructure(player)+
                kingSafety(player)+
                rookStructure(board, player)


                + check(player)
                + majorPiecesInPlay(player)
                + pawnsInCenter(player)
                + pawnsInDeadCenter(player)
                + duoBishop(player)
                + majorIsAttacked(player)
                + enemyHasTwoAttackPawn(board,player);
    }



    private static int attacks(final Player player) {
        int attackScore = 0;
        for(final Move move : player.getLegalMoves()) {
            if(move.isAttack()) {
                final Piece movedPiece = move.getMovedPiece();
                final Piece attackedPiece = move.getAttackedPiece();
                if(movedPiece.getPieceValue() <= attackedPiece.getPieceValue()) {
                    attackScore ++;
                }
            }
        }
        return attackScore * ATTACK_MULTIPLIER;
    }

    private static int pieceEvaluations(final Player player) {
        int pieceValuationScore = 0;
        int numBishops = 0;
        for (final Piece piece : player.getActivePieces()) {
            pieceValuationScore += piece.getPieceValue() + piece.locationBonus();
            if(piece.getPieceType().isBishop()) {
                numBishops++;
            }
        }
        return pieceValuationScore + (numBishops == 2 ? TWO_BISHOPS_BONUS : 0);
    }

    private static int mobility(final Player player) {
        return MOBILITY_MULTIPLIER * mobilityRatio(player);
    }

    private static int mobilityRatio(final Player player) {
        return (int)((player.getLegalMoves().size() * 100.0f) / player.getOpponent().getLegalMoves().size());
    }

    private static int kingThreats(final Player player,
                                   final int depth) {
        return player.getOpponent().isInCheckMate() ? CHECK_MATE_BONUS  * depthBonus(depth) : check(player);
    }

    private static int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    private static int depthBonus(final int depth) {
        return depth == 0 ? 1 : 100 * depth;
    }

    private static int castle(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : castleCapable(player);
    }

    private static int castleCapable(final Player player) {
        return player.isKingSideCastleCapable() || player.isQueenSideCastleCapable() ? CASTLE_CAPABLE_BONUS : 0;
    }

    private static int pawnStructure(final Player player) {
        return PawnStructureAnalyzer.get().pawnStructureScore(player);
    }

    private static int kingSafety(final Player player) {
        final KingDistance kingDistance = KingSafetyAnalyzer.get().calculateKingTropism(player);
        return ((kingDistance.getEnemyPiece().getPieceValue() / 100) * kingDistance.getDistance());
    }

    //it works, just the alpha beta removing all the bad possibilities
    private static int rookStructure(final Board board, final Player player) {
        return RookStructureAnalyzer.get().rookStructureScore(board, player);
    }











    //My stuff /////


    private static int kingStillFirstMove(Player newPlayer, Move theMove){

        if(newPlayer.getPlayerKing().isFirstMove() && !(newPlayer.getPlayerKing().isCastled()) && !(theMove.isCastlingMove())){
            return KING_STILL_FIRST;
        }

        return 0;
    }


    private static int firstMovePawn(Table.MoveLog moveLog, Piece movedPiece, Player newPlayer, Move theMove){

        if(moveLog.size() <= 2 && movedPiece instanceof Pawn){
            if (newPlayer.getAlliance().isBlack() && (theMove.getCurrentCoordinate() == 8 || theMove.getCurrentCoordinate() == 13 || theMove.getCurrentCoordinate() == 14 || theMove.getCurrentCoordinate() == 15)){
                return 0;
            }
            if (newPlayer.getAlliance().isWhite() && (theMove.getCurrentCoordinate() == 55 || theMove.getCurrentCoordinate() == 54 || theMove.getCurrentCoordinate() == 52 || theMove.getCurrentCoordinate() == 48)){
                return 0;
            }
            Random randN = new Random();
            return randN.nextInt(50);
        }
        return 0;
    }




    private static int majorTakesUpTrade(Move theMove, Piece movedPiece) {

        if (( theMove.isAttack()) && !(movedPiece instanceof Queen)
                && (movedPiece instanceof Bishop || movedPiece instanceof Knight || movedPiece instanceof Pawn)
                && (theMove.getAttackedPiece() instanceof Rook || theMove.getAttackedPiece() instanceof Queen)) {

            return UPTRADE_BONUS;
        }

        return 0;
    }






    private static int freeMajors(Move theMove, Player newPlayer){

        if((theMove.isAttack()) && !(theMove.getAttackedPiece() instanceof Pawn)){

            int attacksOnTile = 0;
            for(final Move atkMove : newPlayer.getOpponent().getLegalMoves()){
                if(theMove.getDestinationCoordinate() == atkMove.getDestinationCoordinate()){
                    attacksOnTile++;
                }
            }
            if(attacksOnTile == 0){
                return KILL_THAT_MAJOR_BONUS;
            }

        }
        return 0;
    }



    private static int enemyHasTwoAttackPawn(Board transBoard, Player newPlayer){

        Collection<Piece> enemyPieces = newPlayer.getOpponent().getActivePieces();

        for(Piece piece : enemyPieces){
            if(piece instanceof Pawn ){
                Collection<Move> pawnMoves = piece.calculateLegalMoves(transBoard);
                int Attacks = 0;
                for(Move move: pawnMoves) {
                    //two atks on majors
                    if (move.isAttack() && !(move.getAttackedPiece() instanceof Pawn)) {
                        Attacks++;
                    }
                }
                if(Attacks > 1){
                    return DOUBLE_PAWN_ATTACK_NEGATIVE;
                }
            }
        }
        return 0;
    }






    private static int majorIsAttacked(Player newPlayer) {

        Collection<Piece> myMajors = new ArrayList<>();

        for (Piece piece : newPlayer.getActivePieces()) {
            if (piece instanceof Knight || piece instanceof Bishop || piece instanceof Rook || piece instanceof Queen) {
                myMajors.add(piece);
            }
        }

        for (Piece piece : myMajors) {
            int attacksOnTile = 0;
            for(final Move atkMove : newPlayer.getOpponent().getLegalMoves()){
                if(piece.getPiecePosition() == atkMove.getDestinationCoordinate() && atkMove.getMovedPiece() instanceof Pawn){
                    attacksOnTile++;
                }
            }
            if(attacksOnTile > 0){
                return DONT_DIE_TO_PAWN * attacksOnTile;
            }
        }

        return 0;

    }









    private static int queenNoGetKilled(Move theMove, Piece movedPiece, Player newPlayer) {

        if (movedPiece instanceof Queen) {
            int attacksOnTile = 0;
            for(final Move atkMove : newPlayer.getOpponent().getLegalMoves()){
                if(theMove.getDestinationCoordinate() == atkMove.getDestinationCoordinate()){
                    attacksOnTile++;
                }
            }
            if(attacksOnTile > 0){
                return QUEENS_DO_NOT_DIE;
            }
        }
        return 0;
    }










    private static int tradeWhenAhead(Move theMove, Player newPlayer, Piece movedPiece) {

        Collection<Piece> myPieces = new ArrayList<>();
        Collection<Piece> oppPieces = new ArrayList<>();

        for (Piece piece : newPlayer.getActivePieces()) {
            if (!(piece instanceof Pawn) && !(piece instanceof Queen) && !(piece instanceof Rook)) {
                myPieces.add(piece);
            }
        }

        for (Piece piece : newPlayer.getOpponent().getActivePieces()) {
            if (!(piece instanceof Pawn) && !(piece instanceof Queen) && !(piece instanceof Rook)) {
                oppPieces.add(piece);
            }
        }

        if (myPieces.size() > oppPieces.size()) {

            if (theMove.getAttackedPiece() instanceof Pawn && movedPiece instanceof Pawn) {
                return TRADE_MAJOR_BONUS / 5;
            } else if (theMove.getAttackedPiece() instanceof Knight && movedPiece instanceof Knight) {
                return TRADE_MAJOR_BONUS;
            } else if (theMove.getAttackedPiece() instanceof Bishop && movedPiece instanceof Bishop) {
                return TRADE_MAJOR_BONUS;
            } else if (theMove.getAttackedPiece() instanceof Queen && movedPiece instanceof Queen) {
                return TRADE_MAJOR_BONUS;
            } else if (theMove.getAttackedPiece() instanceof Knight && movedPiece instanceof Bishop) {
                return TRADE_MAJOR_BONUS;
            } else if (theMove.getAttackedPiece() instanceof Bishop && movedPiece instanceof Knight) {
                return TRADE_MAJOR_BONUS;
            } else if (theMove.getAttackedPiece() instanceof Rook && movedPiece instanceof Rook) {
                return TRADE_MAJOR_BONUS;
            }

        }

        return 0;
    }


    public static int tryMoveBishop(Move theMove, Player newPlayer) {

        if(!(theMove.getMovedPiece() instanceof Bishop)){

            Collection<Piece> myPieces = newPlayer.getActivePieces();
            Collection<Piece> myBishops = new ArrayList<>();

            for (Piece piece : myPieces) {
                if (piece instanceof Bishop) {
                    if (piece.isFirstMove()) {
                        myBishops.add(piece);
                    }
                }
            }
            if (myBishops.size() > 0) {
                return MOVE_MAJOR_FIRST;
            }

        }
        return 0;

    }


    public static int tryMoveKnight(Move theMove, Player newPlayer) {

        if(!(theMove.getMovedPiece() instanceof Knight)) {

            Collection<Piece> myPieces = newPlayer.getActivePieces();
            Collection<Piece> myKnights = new ArrayList<>();

            for (Piece piece : myPieces) {
                if (piece instanceof Knight) {
                    if (piece.isFirstMove()) {
                        myKnights.add(piece);
                    }
                }
            }
            if (myKnights.size() > 0) {
                return MOVE_MAJOR_FIRST;
            }
        }

        return 0;

    }







    private static int majorNoGetKilled(Move theMove, Piece movedPiece, Player newPlayer) {

        Collection<Move> attacks = new ArrayList<>();

        if (!(movedPiece instanceof Pawn)) {

            for(final Move atkMove : newPlayer.getOpponent().getLegalMoves()){
                if(theMove.getDestinationCoordinate() == atkMove.getDestinationCoordinate()){
                    attacks.add(atkMove);
                }
            }

            for (Move atkMoves : attacks) {
                if (atkMoves.getMovedPiece() instanceof Pawn) {
                    return DONT_DIE_TO_PAWN;
                }
            }

            for (Move atkMoves : attacks) {
                if (atkMoves.getMovedPiece().getPieceValue() < theMove.getMovedPiece().getPieceValue()) {
                    return NOT_GOOD_TRADE;
                }
            }


        }

        return 0;
    }


    private static int kingProximity(Board transBoard, Player newPlayer, Move theMove) {

        King king = newPlayer.getPlayerKing();
        int kingPosition = king.getPiecePosition();
        int nearbyAlly = 0;

        if(theMove.isCastlingMove()) {

            if (newPlayer.getAlliance().isBlack()) {

                int[] PROXIMITY_VECTOR = {1, 7, 8, 9};
                for (int vectors : PROXIMITY_VECTOR) {
                    if ((kingPosition + vectors) < 64 && transBoard.getTile(kingPosition + vectors).isTileOccupied()
                            && transBoard.getTile(kingPosition + vectors).getPiece().getPieceAllegiance() == king.getPieceAllegiance()) {
                        nearbyAlly++;
                    }
                }

                int[] PROXIMITY_VECTOR2 = {1};
                for (int vectors : PROXIMITY_VECTOR2) {
                    if ((kingPosition - vectors) >= 0 && transBoard.getTile(kingPosition - vectors).isTileOccupied()
                            && transBoard.getTile(kingPosition - vectors).getPiece().getPieceAllegiance() == king.getPieceAllegiance()) {
                        nearbyAlly++;
                    }
                }


            } else {

                int[] PROXIMITY_VECTOR = {1};
                for (int vectors : PROXIMITY_VECTOR) {
                    if ((kingPosition + vectors) < 64 && transBoard.getTile(kingPosition + vectors).isTileOccupied()
                            && transBoard.getTile(kingPosition + vectors).getPiece().getPieceAllegiance() == king.getPieceAllegiance()) {
                        nearbyAlly++;
                    }
                }

                int[] PROXIMITY_VECTOR2 = {1, 7, 8, 9};
                for (int vectors : PROXIMITY_VECTOR2) {
                    if ((kingPosition - vectors) >= 0 && transBoard.getTile(kingPosition - vectors).isTileOccupied()
                            && transBoard.getTile(kingPosition - vectors).getPiece().getPieceAllegiance() == king.getPieceAllegiance()) {
                        nearbyAlly++;
                    }
                }

            }

        }

        return nearbyAlly * KING_ALLIES_BONUS;

    }






    private static int duoBishop(Player newPlayer) {

        int countBishops = 0;
        if ((newPlayer.getActivePieces().size() + newPlayer.getOpponent().getActivePieces().size()) < 27) {
            for (Piece piece : newPlayer.getActivePieces()) {
                if (piece instanceof Bishop) {
                    countBishops++;
                }
            }
            return countBishops > 1 ? BISHOP_DUO_BONUS : 0;
        } else {
            for (Piece piece : newPlayer.getActivePieces()) {
                if (piece instanceof Bishop) {
                    countBishops++;
                }
            }
            return countBishops > 1 ? BISHOP_DUO_BONUS / 4 : 0;
        }

    }


    private static int pawnsInCenter(Player newPlayer) {

        List<Integer> centerPositions = new ArrayList<>();
        Collections.addAll(centerPositions, 18,19,20,21,26,29,34,37,42,43,44,45);

        int countPieces = 0;
        for (Piece piece : newPlayer.getActivePieces()) {
            if (piece instanceof Pawn && centerPositions.contains(piece.getPiecePosition())) {
                countPieces++;
            }
        }

        return countPieces * PAWN_IN_CENTER_BONUS;

    }


    private static int pawnsInDeadCenter(Player newPlayer) {

        List<Integer> centerPositions = new ArrayList<>();
        Collections.addAll(centerPositions,27,28,35,36);

        int countPieces = 0;
        for (Piece piece : newPlayer.getActivePieces()) {
            if (piece instanceof Pawn && centerPositions.contains(piece.getPiecePosition())) {
                countPieces++;
            }
        }
        return countPieces * PAWN_IN_CENTER_BONUS * 2;
    }



    private static int majorPiecesInPlay(Player newPlayer) {

        List<Integer> centerPositions = new ArrayList<>();
        Collections.addAll(centerPositions, 9,10,11,12,13,14,17,18,19,20,21,22,25,26,27,28,29,30,33,34,35,36,37,38,41,42,43,44,45,46,49,50,51,52,53,54);

        int countMajors = 0;
        for (Piece piece : newPlayer.getActivePieces()) {
            if ((piece instanceof Bishop || piece instanceof Knight) && centerPositions.contains(piece.getPiecePosition())) {
                countMajors++;
            }
        }
        return countMajors * MAJOR_IN_PLAY_BONUS;
    }


    private static int castled(Player newPlayer) {
        return newPlayer.isCastled() ? CASTLED_BONUS : 0;
    }




    private static int pieceValue(final Player newPlayer) {
        int pieceValueScore = 0;
        for (final Piece piece : newPlayer.getActivePieces()) {
            pieceValueScore += piece.getPieceValue();
        }
        return pieceValueScore;
    }
















}
