package com.chess.engine.player.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.gui.Table;

public interface MoveStrategy {


    Move execute(Board board);


}
