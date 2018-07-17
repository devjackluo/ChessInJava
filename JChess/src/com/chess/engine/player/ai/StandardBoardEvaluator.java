package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.pieces.*;
import com.chess.engine.player.Player;


/**
 *
 * A board evaulator implements a boardevaulator interface (although kind of unnessary atm)
 * No constructor
 *
 * Evaluate function takes the score of the white player - the black player
 * Evauluate function is passed a board to evaluated and a current stage depth
 *
 * */

public final class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_BONUS = 8;
    private static final int CASTLED_BONUS = 40;


    @Override
    public int evaluate(final Board board, final int depth) {
        return scorePlayer(board.whitePlayer(), depth) - scorePlayer(board.blackPlayer(), depth);
    }

    private int scorePlayer(final Player player, int depth) {

        return pieceValue(player)
                + check(player)
                + checkMate(player)
                + castled(player)
                + mobility(player)
                + futureError(depth);
    }


    private static int castled(Player player) {
        return player.isCastled() ? CASTLED_BONUS : 0;
    }

    private static int futureError(final int depth) {
        return depth == 0 ? 1 : 100 * depth;
    }

    private static int check(Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    private static int checkMate(Player player) {
        return player.getOpponent().isInCheckMate() ? CHECK_BONUS*2 : 0;
    }

    private static int mobility(final Player player) {
        return player.getLegalMoves().size();
    }

    private static int pieceValue(final Player player) {
        int pieceValueScore = 0;
        for (final Piece piece : player.getActivePieces()) {
            pieceValueScore += piece.getPieceValue();
        }
        return pieceValueScore;
    }


}
