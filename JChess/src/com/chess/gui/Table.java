package com.chess.gui;

import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Tile;
import com.chess.engine.pieces.King;
import com.chess.engine.pieces.Piece;
import com.chess.engine.pieces.Queen;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.Player;
import com.chess.engine.player.ai.*;
import com.chess.pgn.FenUtilities;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import javafx.scene.control.Tab;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
//import java.lang.Runnable;

import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isLeftMouseButton;

import java.sql.*;


public class Table extends Observable {

    private final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final BoardPanel boardPanel;

    private final MoveLog moveLog;

    private final GameSetup gameSetup;

    private Board chessBoard;


    private Tile sourceTile;
    private Tile destinationTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;

    private Move computerMove;

    private boolean highlightLegalMoves;

    private final static Dimension OUTER_FRAME_DIMENSION = new Dimension(720, 600);
    private final static Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
    private final static Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static String defaultPieceImagesPath = "ChessArt/";

    private static int loop = 0;


    private final Color lightTileColor = Color.decode("0X97B3E6");
    private final Color darkTileColor = Color.decode("0X155bdb");

    private static final Table INSTANCE = new Table();

    private boolean useTable = false;
    private String bestState = "";

    private Table() {

        this.gameFrame = new JFrame("JChess");
        this.gameFrame.setLayout(new BorderLayout());
        final JMenuBar tableMenuBar = createTableMenuBar();
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);

        this.chessBoard = Board.createStandardBoard();

        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();

        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();

        this.addObserver(new TableGameAIWatcher());

        this.gameSetup = new GameSetup(this.gameFrame, true);

        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = true;


        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);
        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);

        this.gameFrame.setVisible(true);

    }


    public static Table get() {
        return INSTANCE;
    }


    public void show() {

        invokeLater(() -> {

            //Get MoveLog
            Table.get().getMoveLog().clear();
            //Takes current chessboard and move log and does redo function
            //redraws move history
            Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());

            //TODO Document taken pieces class
            // just refreshes pices panel
            Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());

            //populate board with tilespanel and draws board with colors and pieces
            Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());

        });

    }


    private GameSetup getGameSetup() {
        return this.gameSetup;
    }

    private Board getGameBoard() {
        return this.chessBoard;
    }

    private void setGameBoard(Board board) {
        this.chessBoard = board;
    }

    private void setUseTable(boolean bool) {
        this.useTable = useTable;
    }

    private void setBestFen(String fen) {
        this.bestState = fen;
    }

    private JMenuBar createTableMenuBar() {
        final JMenuBar tableMenuBar = new JMenuBar();
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
        return tableMenuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");

        final JMenuItem openPGN = new JMenuItem("Load PGN File");
        openPGN.addActionListener(e -> System.out.println("Open up that pgn file!"));

        fileMenu.add(openPGN);

        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> System.exit(0));

        fileMenu.add(exitMenuItem);

        return fileMenu;

    }

    private JMenu createPreferencesMenu() {

        final JMenu preferencesMenu = new JMenu("Preferences");
        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip Board");
        flipBoardMenuItem.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            boardPanel.drawBoard(chessBoard);
        });

        preferencesMenu.add(flipBoardMenuItem);

        preferencesMenu.addSeparator();

        final JCheckBoxMenuItem legalMoveHighlighterCheckbox = new JCheckBoxMenuItem("Highlight Legal Moves", false);
        legalMoveHighlighterCheckbox.setState(true);

        legalMoveHighlighterCheckbox.addActionListener(e -> {
            highlightLegalMoves = legalMoveHighlighterCheckbox.isSelected();
            gameHistoryPanel.redo(chessBoard, moveLog);
            takenPiecesPanel.redo(moveLog);
            boardPanel.drawBoard(chessBoard);
        });

        preferencesMenu.add(legalMoveHighlighterCheckbox);

        return preferencesMenu;

    }

    private JMenu createOptionsMenu() {

        final JMenu optionMenu = new JMenu("Options");

        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game");
        setupGameMenuItem.addActionListener(e -> {

            sourceTile = null;
            destinationTile = null;
            humanMovedPiece = null;
            boardPanel.drawBoard(chessBoard);


            Table.get().getGameSetup().promptUser();
            Table.get().setupUpdate(Table.get().getGameSetup());

        });

        optionMenu.add(setupGameMenuItem);


        final JMenuItem newGameMenuItem = new JMenuItem("New Game");
        newGameMenuItem.addActionListener(e -> {
            undoAllMoves();
        });

        optionMenu.add(newGameMenuItem);


        return optionMenu;

    }

    private void undoAllMoves() {

        Table.get().getGameSetup().setWhitePlayerType(PlayerType.HUMAN);
        Table.get().getGameSetup().setBlackPlayerType(PlayerType.HUMAN);

        for(int i = Table.get().getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getTransitionBoard();
        }
        this.computerMove = null;
        Table.get().getMoveLog().clear();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(chessBoard);
        //Table.get().getDebugPanel().redo();
    }

    private void setupUpdate(final GameSetup gameSetup) {

        //or refresh here

        setChanged();
        notifyObservers(gameSetup);
    }


    /**
     * Created during init
     * uses obserable
     * obserable has a update function
     */
    private static class TableGameAIWatcher implements Observer {

        @Override
        public void update(final Observable o, final Object arg) {

            //if current player is AI, create AIThinkTank()
            if (Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer()) &&
                    !Table.get().getGameBoard().currentPlayer().isInCheckMate() &&
                    !Table.get().getGameBoard().currentPlayer().isInStaleMate()) {

                //create an AI thread
                //execute ai work
                final AIThinkTank thinkTank = new AIThinkTank();
                thinkTank.execute();

//                final PGNThinkTank thinkTank = new PGNThinkTank();
//                thinkTank.execute();
            }

            if (Table.get().getGameBoard().currentPlayer().isInCheckMate()) {
                System.out.println("Game Over, " + Table.get().getGameBoard().currentPlayer() + " is in Checkmate");
            }

            if (Table.get().getGameBoard().currentPlayer().isInStaleMate()) {
                System.out.println("Game Over, " + Table.get().getGameBoard().currentPlayer() + " is in Stalemate");
            }

        }

    }


    public void updateGameBoard(final Board board) {
        this.chessBoard = board;
    }

    public void updateComputerMove(final Move move) {
        this.computerMove = move;
    }

    private MoveLog getMoveLog() {
        return this.moveLog;
    }

    private GameHistoryPanel getGameHistoryPanel() {
        return this.gameHistoryPanel;
    }

    private TakenPiecesPanel getTakenPiecesPanel() {
        return this.takenPiecesPanel;
    }

    private BoardPanel getBoardPanel() {
        return this.boardPanel;
    }

    private void moveMadeUpdate(final PlayerType playerType) {
        setChanged();
        notifyObservers(playerType);
    }


    /**
     * Swingworker does background taskes for GUIs
     * <p>
     * Swingworker<T,V>
     * T = Variable to return from doinbackground
     * V = publish and process methos (didn't use so we just do string)
     * <p>
     * We call minimax for the AI with the current board
     */
    private static class AIThinkTank extends SwingWorker<Move, String> {

        private AIThinkTank() {

        }

        @Override
        protected Move doInBackground() throws Exception {


            String currentBoard = FenUtilities.createFENFromGame(Table.get().chessBoard);

            Connection c = null;
            Statement stmt = null;

            try {

                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:test.db");
                c.setAutoCommit(false);

                stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM BESTCACHE WHERE STATE = '" + currentBoard + "'ORDER BY DEPTH DESC LIMIT 1");

                if (rs.next()) {

                    int depth = rs.getInt("DEPTH");

                    if (depth >= Table.get().gameSetup.getSearchDepth()) {

                        Table.get().bestState = rs.getString("BESTSTATE");
                        rs.close();
                        stmt.close();
                        c.close();
                        Table.get().useTable = true;
                        int minimaxDepth = 1;
                        final MoveStrategy miniMax = new AlphaBeta(minimaxDepth);
                        return miniMax.execute(Table.get().getGameBoard());

                    } else {

                        System.out.println("DELETING OLD");

                        try {
                            stmt = c.createStatement();
                            String sql = "DELETE from BESTCACHE where STATE='" + FenUtilities.createFENFromGame(Table.get().chessBoard) + "';";
                            stmt.executeUpdate(sql);
                            c.commit();
                        } catch (Exception e) {
                            System.err.println(e.getClass().getName() + ": " + e.getMessage());
                            System.exit(0);
                        }


                    }

                }

                rs.close();
                stmt.close();
                c.close();

            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }


            int minimaxDepth = Table.get().gameSetup.getSearchDepth();

            final MoveStrategy miniMax = new AlphaBetaThreadTwo(minimaxDepth);

            return miniMax.execute(Table.get().getGameBoard());

        }

        @Override
        public void done() {

            try {

                if (Table.get().useTable) {

                    final Move bestMove = get();
                    Table.get().updateGameBoard(FenUtilities.createGameFromFEN(Table.get().bestState));
                    Table.get().getMoveLog().addMove(bestMove);
                    Table.get().getGameHistoryPanel().redo(Table.get().getGameBoard(), Table.get().getMoveLog());
                    Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
                    Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
                    Table.get().moveMadeUpdate(PlayerType.COMPUTER);
                    Table.get().useTable = false;


                } else {

                    //when doinbackground is done, 'swings' over to here and we can now get() the return value
                    final Move bestMove = get();

                    Table.get().updateComputerMove(bestMove);

                    String oldBoard = FenUtilities.createFENFromGame(Table.get().chessBoard);
                    Board bestBoard = Table.get().getGameBoard().currentPlayer().makeMove(bestMove).getTransitionBoard();
                    String newBoard = FenUtilities.createFENFromGame(bestBoard);


                    Connection c = null;
                    Statement stmt = null;

                    try {
                        Class.forName("org.sqlite.JDBC");
                        c = DriverManager.getConnection("jdbc:sqlite:test.db");
                        c.setAutoCommit(false);

                        stmt = c.createStatement();
                        String sql = "INSERT INTO BESTCACHE (DEPTH,STATE,BESTSTATE) " +
                                "VALUES ( " + Table.get().gameSetup.getSearchDepth() + ",'" + oldBoard + "', '" + newBoard + "');";

                        stmt.executeUpdate(sql);
                        stmt.close();
                        c.commit();
                        c.close();


                    } catch (Exception e) {
                        System.exit(0);
                    }



                    Table.get().updateGameBoard(bestBoard);
                    Table.get().getMoveLog().addMove(bestMove);
                    Table.get().getGameHistoryPanel().redo(Table.get().getGameBoard(), Table.get().getMoveLog());
                    Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
                    Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
                    Table.get().moveMadeUpdate(PlayerType.COMPUTER);

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

    }


    public List<String> pgnMoves = new ArrayList<>();

    public synchronized List<String> getPgnMoves() {
        return pgnMoves;
    }

    private static class PGNThinkTank extends SwingWorker<Move, String> {



        private PGNThinkTank() {

            if(Table.get().getPgnMoves().isEmpty()) {

                String png = "1.e4 e5 2.Nf3 Nc6 3.d4 exd4 4.Nxd4 Nf6 5.Nc3 Bb4 6.Nxc6 bxc6 7.Qd3 O-O 8.Bd2 Bxc3\n" +
                        "9.Bxc3 Nxe4 10.Qxe4 Re8 11.Be5 f6 12.O-O-O fxe5 13.Bd3 Qg5+ 14.Kb1 Qh6 15.c4 Bb7\n" +
                        "16.Qf5 d5 17.g4 Bc8 18.Qh5 Qxh5 19.gxh5 Bg4 20.Rc1 e4 21.Bf1 d4 22.h6 g6\n" +
                        "23.Rg1 Bh5 24.Kc2 Rf8 25.Rg2 Rf5 26.Re1 Raf8 27.Rxe4 Bf3 28.Kd3 Bxe4+ 29.Kxe4 Rf4+  0-1";

                png = png.replaceAll("(\\d?)(\\d+)\\.", "");
                png = png.replaceAll("\n", " ");

                String[] parts = png.split(" ");

                for(int i = 0; i < parts.length-2; i++){
                    Table.get().pgnMoves.add(parts[i]);
                }

            }

        }

        @Override
        protected Move doInBackground() throws Exception {

            String pgnMove = Table.get().pgnMoves.get(0);
            Move bestMove = null;
            List<Move> moves = new ArrayList<>();

//            if(Table.get().pgnMoves.get(0).equals("Qg5+")){
//                System.out.println("Ok");
//            }

            for (final Move move : Table.get().chessBoard.currentPlayer().getLegalMoves()) {

                final MoveTransition moveTransition = Table.get().chessBoard.currentPlayer().makeMove(move);

                if (moveTransition.getMoveStatus().isDone()) {

                    moves.add(move);

                    if(move.toString().equals(pgnMove)){
                        bestMove = move;
                    }

                }
            }


            System.out.println(bestMove.toString());


            return bestMove;

        }

        @Override
        public void done() {


            try {

                final Move bestMove = get();

                Table.get().updateComputerMove(bestMove);
                Board bestBoard = Table.get().getGameBoard().currentPlayer().makeMove(bestMove).getTransitionBoard();
                Table.get().updateGameBoard(bestBoard);
                Table.get().getMoveLog().addMove(bestMove);
                Table.get().getGameHistoryPanel().redo(Table.get().getGameBoard(), Table.get().getMoveLog());
                Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
                Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
                Table.get().moveMadeUpdate(PlayerType.COMPUTER);
                Table.get().pgnMoves.remove(0);

                if(Table.get().pgnMoves.isEmpty()){
                    Table.get().getGameSetup().setWhitePlayerType(PlayerType.HUMAN);
                    Table.get().getGameSetup().setBlackPlayerType(PlayerType.HUMAN);
                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }



        }

    }


    public enum BoardDirection {

        NORMAL {
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);

        abstract BoardDirection opposite();

    }

    /**
     * Creates a list of TilePanels for each grid #(0-63)
     */
    private class BoardPanel extends JPanel {
        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8, 8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            validate();
        }

        public void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel tilePanel : boardDirection.traverse(boardTiles)) {
                tilePanel.drawTile(board);
                add(tilePanel);
            }
            validate();
            repaint();
        }

    }

    /**
     * MoveLog class creates hold a empty list of Moves that can me manipulated
     */
    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return moves;
        }

        public void addMove(final Move move) {
            this.moves.add(move);
        }

        public int size() {
            return this.moves.size();
        }

        public void clear() {
            this.moves.clear();
        }

        public Move removeMove(int index) {
            return this.moves.remove(index);
        }

        public boolean removeMove(final Move move) {
            return this.moves.remove(move);
        }

    }


    enum PlayerType {
        HUMAN,
        COMPUTER
    }


    /**
     * For each TilePanel, retain tile id
     */
    private class TilePanel extends JPanel {

        private final int tileId;

        TilePanel(final BoardPanel boardPanel, final int tileId) {

            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);

            //does exactly what name of function says.
            assignTileColor();
            assignTilePieceIcon(chessBoard);

            // listener checks if current play is human and if he is, do all teh checks to show moves etc
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {

                    if ((chessBoard.currentPlayer().isWhite() && gameSetup.getWhitePlayerType() == PlayerType.HUMAN)
                            || (chessBoard.currentPlayer().isBlack() && gameSetup.getBlackPlayerType() == PlayerType.HUMAN)) {


//                        if (isRightMouseButton(e)) {
//
//                            sourceTile = null;
//                            destinationTile = null;
//                            humanMovedPiece = null;
//
//
//                        } else


                        //if left click
                        if (isLeftMouseButton(e)) {

                            //if no tile selected
                            if (sourceTile == null) {


                                sourceTile = chessBoard.getTile(tileId);
                                humanMovedPiece = sourceTile.getPiece();
                                if (humanMovedPiece == null) {
                                    sourceTile = null;
                                }


                            } else {

                                //check if valid click, else this is new source piece
                                boolean wasLegal = false;
                                destinationTile = chessBoard.getTile(tileId);
                                for (final Move moves : sourceTile.getPiece().calculateLegalMoves(chessBoard)) {
                                    if (destinationTile.getTileCoordinate() == moves.getDestinationCoordinate()) {
                                        //System.out.println(moves.getCurrentCoordinate());
                                        //System.out.println(moves.getDestinationCoordinate());
                                        wasLegal = true;
                                    }
                                }


                                //special case if its was the king selected then also check of the destination was a legal castle move.
                                if (sourceTile.getPiece() instanceof King) {
                                    for (final Move moves : chessBoard.currentPlayer().getCastleMoves()) {
                                        if (destinationTile.getTileCoordinate() == moves.getDestinationCoordinate()) {
                                            wasLegal = true;
                                        }
                                    }
                                }


                                if (wasLegal) {
                                    final Move move = Move.MoveFactory.createMove(chessBoard, sourceTile.getTileCoordinate(), destinationTile.getTileCoordinate());
                                    final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                                    if (transition.getMoveStatus().isDone()) {
                                        chessBoard = transition.getTransitionBoard();
                                        moveLog.addMove(move);

                                    }
                                    sourceTile = null;
                                    destinationTile = null;
                                    humanMovedPiece = null;
                                } else {
                                    sourceTile = chessBoard.getTile(tileId);
                                    humanMovedPiece = sourceTile.getPiece();
                                    if (humanMovedPiece == null) {

                                        sourceTile = null;
                                    }
                                }


                            }

                            invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    gameHistoryPanel.redo(chessBoard, moveLog);
                                    takenPiecesPanel.redo(moveLog);

                                    if (gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
                                        Table.get().moveMadeUpdate(PlayerType.HUMAN);
                                    }

                                    boardPanel.drawBoard(chessBoard);


                                    if (sourceTile != null && sourceTile.getPiece() instanceof Piece && sourceTile.getPiece().getPieceAlliance() == chessBoard.currentPlayer().getAlliance()) {
                                        try {

                                            GridBagConstraints constraints = new GridBagConstraints();
                                            constraints.gridx = 0;
                                            constraints.gridy = 0;
                                            constraints.anchor = GridBagConstraints.CENTER;
                                            boardPanel.boardTiles.get(tileId).add(new JLabel(new ImageIcon(ImageIO.read(new File("ChessArt/red_square.png")))), constraints);
                                            boardPanel.boardTiles.get(tileId).validate();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }
                            });


                        }

                    }

                }

                @Override
                public void mousePressed(final MouseEvent e) {

                }

                @Override
                public void mouseReleased(final MouseEvent e) {

                }

                @Override
                public void mouseEntered(final MouseEvent e) {

                }

                @Override
                public void mouseExited(final MouseEvent e) {

                }
            });

            validate();
        }

        public void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightLegals(board);
            validate();
            repaint();
        }

        private void assignTilePieceIcon(final Board board) {

            this.removeAll();
            if (board.getTile(this.tileId).isTileOccupied()) {
                try {
                    final BufferedImage image = ImageIO.read(new File(defaultPieceImagesPath + board.getTile(this.tileId).getPiece().getPieceAlliance().toString().substring(0, 1) + board.getTile(this.tileId).getPiece().toString() + ".png"));
                    add(new JLabel(new ImageIcon(image)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


        private void highlightLegals(final Board board) {

            if ((chessBoard.currentPlayer().isWhite() && gameSetup.getWhitePlayerType() == PlayerType.HUMAN) || (chessBoard.currentPlayer().isBlack() && gameSetup.getBlackPlayerType() == PlayerType.HUMAN)) {

                if (highlightLegalMoves) {
                    for (final Move move : pieceLegalMoves(board)) {
                        if (move.getDestinationCoordinate() == this.tileId) {
                            try {

                                GridBagConstraints constraints = new GridBagConstraints();
                                constraints.gridx = 0;
                                constraints.gridy = 0;
                                constraints.anchor = GridBagConstraints.CENTER;
                                add(new JLabel(new ImageIcon(ImageIO.read(new File("ChessArt/red_square.png")))), constraints);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                }

            }

        }


        private Collection<Move> pieceLegalMoves(final Board board) {
            if (humanMovedPiece != null && humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()) {
                if (!(humanMovedPiece instanceof King)) {
                    return humanMovedPiece.calculateLegalMoves(board);
                } else {
                    return ImmutableList.copyOf(Iterables.concat(humanMovedPiece.calculateLegalMoves(board), board.currentPlayer().getCastleMoves()));
                }
            }
            return Collections.emptyList();
        }

        private void assignTileColor() {
            if (BoardUtils.EIGHTH_RANK[this.tileId] || BoardUtils.SIXTH_RANK[this.tileId] || BoardUtils.FOURTH_RANK[this.tileId] || BoardUtils.SECOND_RANK[this.tileId]) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if (BoardUtils.SEVENTH_RANK[this.tileId] || BoardUtils.FIFTH_RANK[this.tileId] || BoardUtils.THIRD_RANK[this.tileId] || BoardUtils.FIRST_RANK[this.tileId]) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }

        }

    }

}
