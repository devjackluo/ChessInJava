package com.chess;

import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.player.MoveTransition;
import com.chess.engine.player.ai.StandardBoardEvaluator;
import com.chess.gui.Table;
import com.chess.pgnParser.PGNParser;
import javafx.util.Pair;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PGNChess {

    public static void main(String[] args){

        List<String> fileList = new ArrayList<>();
        fileList.add("./pgn/Anand.pgn");
        fileList.add("./pgn/Capablanca.pgn");
        fileList.add("./pgn/Carlsen.pgn");
        fileList.add("./pgn/Fischer.pgn");
        fileList.add("./pgn/Karpov.pgn");
        fileList.add("./pgn/Kasparov.pgn");
        fileList.add("./pgn/Morphy.pgn");
        fileList.add("./pgn/Spassky.pgn");
        fileList.add("./pgn/Tal.pgn");
        fileList.add("./pgn/WCC.pgn");


        for(String filePath : fileList) {
            try {
                List<String> pgnStrings = PGNParser.getPGNString(filePath);
                for (String s : pgnStrings) {
                    playOutPGNPiece(s);
                    playOutPGNFile(s);
                    playOutPGNRank(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void playOutPGN(String pgn) {

        List<String> pgnMoves = new ArrayList<>();

        pgn = pgn.replaceAll("(\\d?)(\\d+)\\.", "");
        pgn = pgn.replaceAll("\n", " ");
        pgn = pgn.replaceAll("  ", " ");

        String[] parts = pgn.split(" ");

        boolean black = false;
        boolean white = false;
        String winner = parts[parts.length-1];
        String[] winners = winner.split("-");
        if(winners[0].contains("0")){
            black = true;
        }else if (winners[1].contains("0")){
            white = true;
        }


        for (int i = 0; i < parts.length - 2; i++) {
            pgnMoves.add(parts[i]);
        }

        Board board = Board.createStandardBoard();

        //System.out.println(pgnMoves.size());
        boolean goodData = true;

        Map<Integer, Pair<List<Integer>, Integer>> inputOutputMap = new HashMap<>();

        StandardBoardEvaluator eval = new StandardBoardEvaluator();

        for (int i = 0; i < pgnMoves.size(); i++) {


            String pgnMove = pgnMoves.get(i);
            Move bestMove = null;
            int bestMovePosition = 0;
            List<Integer> possibleMoves = new ArrayList<>();

            int count = 0;
            for (final Move move : board.currentPlayer().getLegalMoves()) {

                final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

                if (moveTransition.getMoveStatus().isDone()) {

                    if(move.getAttackedPiece() != null){
                        //value to differrentiate moves
                        possibleMoves.add(move.getCurrentCoordinate()
                                + move.getDestinationCoordinate()
                                + move.getMovedPiece().getPieceValue()
                                + i
                                + move.getAttackedPiece().getPieceValue()
                                + eval.evaluate(moveTransition.getTransitionBoard(), 1)
                        );
                    }else{
                        //value to differrentiate moves
                        possibleMoves.add(move.getCurrentCoordinate()
                                + move.getDestinationCoordinate()
                                + move.getMovedPiece().getPieceValue()
                                + i
                                + eval.evaluate(moveTransition.getTransitionBoard(), 1)
                        );
                    }



                    if (move.toString().equals(pgnMove)) {
                        bestMove = move;
                        bestMovePosition = count;
                    }

                    count++;

                }
            }

            if(bestMove == null){
                System.out.println("Fuck! " + pgnMove + " at move" + i);
                goodData = false;
                break;
            }

            if(white && board.currentPlayer().getAlliance().isWhite()){
                Pair<List<Integer>, Integer> inout = new Pair<>(possibleMoves, bestMovePosition);
                inputOutputMap.put(i, inout);
            }else if (black && board.currentPlayer().getAlliance().isBlack()) {
                Pair<List<Integer>, Integer> inout = new Pair<>(possibleMoves, bestMovePosition);
                inputOutputMap.put(i, inout);
            }

                //System.out.println(possibleMoves);
            //pgnMoves.remove(0);
            //System.out.println(board.currentPlayer().getAlliance().toString() + "-" + bestMove);
            //System.out.println(pgnMoves.get(0));
            board = board.currentPlayer().makeMove(bestMove).getTransitionBoard();
            //System.out.println(board);


        }

        if(goodData) {
//            if (white) {
//                System.out.println("White Wins");
//            } else if (black) {
//                System.out.println("Black Wins");
//            } else {
//                System.out.println("Draw");
//            }

            File file = new File("./TrainingData/test.txt");
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);

                for(Map.Entry<Integer, Pair<List<Integer>, Integer>> entry : inputOutputMap.entrySet()){
                    bw.write(entry.getValue().toString() + "\n");
                }
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //System.out.println(board);

    }

    public static void playOutPGNPiece(String pgn) {

        List<String> pgnMoves = new ArrayList<>();

        pgn = pgn.replaceAll("(\\d?)(\\d+)\\.", "");
        pgn = pgn.replaceAll("\n", " ");
        pgn = pgn.replaceAll("  ", " ");

        String[] parts = pgn.split(" ");

        boolean black = false;
        boolean white = false;
        String winner = parts[parts.length-1];
        String[] winners = winner.split("-");
        if(winners[0].contains("0")){
            black = true;
        }else if (winners[1].contains("0")){
            white = true;
        }


        for (int i = 0; i < parts.length - 2; i++) {
            pgnMoves.add(parts[i]);
        }

        Board board = Board.createStandardBoard();

        boolean goodData = true;

        Map<Integer, Pair<List<Integer>, Integer>> inputOutputMap = new HashMap<>();

        for (int i = 0; i < pgnMoves.size(); i++) {

            String pgnMove = pgnMoves.get(i);
            Move bestMove = null;
            int bestMovePiece = 0;
            List<Integer> boardState = new ArrayList<>();

            for(int b = 0; b < BoardUtils.NUM_TILES; b++){
                if(board.currentPlayer().isBlack()) {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(-board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }else {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }
            }


            for (final Move move : board.currentPlayer().getLegalMoves()) {

                final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

                if (moveTransition.getMoveStatus().isDone()) {

                    if (move.toString().equals(pgnMove)) {
                        bestMove = move;
                        switch (move.getMovedPiece().getPieceType().toString()){
                            case "K":
                                bestMovePiece = 1;
                                break;
                            case "Q":
                                bestMovePiece = 2;
                                break;
                            case "R":
                                bestMovePiece = 3;
                                break;
                            case "B":
                                bestMovePiece = 4;
                                break;
                            case "N":
                                bestMovePiece = 5;
                                break;
                            case "P":
                                bestMovePiece = 6;
                                break;
                        }
                    }
                }
            }

            if(bestMove == null){
                System.out.println("Fuck! " + pgnMove + " at move" + i);
                goodData = false;
                break;
            }

            if(white && board.currentPlayer().getAlliance().isWhite()){
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMovePiece);
                inputOutputMap.put(i, inout);
            }else if (black && board.currentPlayer().getAlliance().isBlack()) {
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMovePiece);
                inputOutputMap.put(i, inout);
            }

            board = board.currentPlayer().makeMove(bestMove).getTransitionBoard();

        }

        if(goodData) {
            File file = new File("./TrainingData/piece.txt");
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);

                for(Map.Entry<Integer, Pair<List<Integer>, Integer>> entry : inputOutputMap.entrySet()){
                    bw.write(entry.getValue().toString() + "\n");
                }
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void playOutPGNFile(String pgn) {

        List<String> pgnMoves = new ArrayList<>();

        pgn = pgn.replaceAll("(\\d?)(\\d+)\\.", "");
        pgn = pgn.replaceAll("\n", " ");
        pgn = pgn.replaceAll("  ", " ");

        String[] parts = pgn.split(" ");

        boolean black = false;
        boolean white = false;
        String winner = parts[parts.length-1];
        String[] winners = winner.split("-");
        if(winners[0].contains("0")){
            black = true;
        }else if (winners[1].contains("0")){
            white = true;
        }

        for (int i = 0; i < parts.length - 2; i++) {
            pgnMoves.add(parts[i]);
        }

        Board board = Board.createStandardBoard();
        boolean goodData = true;
        Map<Integer, Pair<List<Integer>, Integer>> inputOutputMap = new HashMap<>();

        for (int i = 0; i < pgnMoves.size(); i++) {

            String pgnMove = pgnMoves.get(i);
            Move bestMove = null;
            int bestMoveFile = 0;
            List<Integer> boardState = new ArrayList<>();

            for(int b = 0; b < BoardUtils.NUM_TILES; b++){
                if(board.currentPlayer().isBlack()) {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(-board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }else {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }
            }

            for (final Move move : board.currentPlayer().getLegalMoves()) {

                final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

                if (moveTransition.getMoveStatus().isDone()) {

                    if (move.toString().equals(pgnMove)) {
                        bestMove = move;
                        bestMoveFile = fileToNum(move.getDestinationCoordinate());
                    }
                }
            }

            if(bestMove == null){
                System.out.println("Fuck! " + pgnMove + " at move" + i);
                goodData = false;
                break;
            }

            if(white && board.currentPlayer().getAlliance().isWhite()){
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMoveFile);
                inputOutputMap.put(i, inout);
            }else if (black && board.currentPlayer().getAlliance().isBlack()) {
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMoveFile);
                inputOutputMap.put(i, inout);
            }
            board = board.currentPlayer().makeMove(bestMove).getTransitionBoard();

        }

        if(goodData) {
            File file = new File("./TrainingData/file.txt");
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);

                for(Map.Entry<Integer, Pair<List<Integer>, Integer>> entry : inputOutputMap.entrySet()){
                    bw.write(entry.getValue().toString() + "\n");
                }
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void playOutPGNRank(String pgn) {

        List<String> pgnMoves = new ArrayList<>();

        pgn = pgn.replaceAll("(\\d?)(\\d+)\\.", "");
        pgn = pgn.replaceAll("\n", " ");
        pgn = pgn.replaceAll("  ", " ");

        String[] parts = pgn.split(" ");

        boolean black = false;
        boolean white = false;
        String winner = parts[parts.length-1];
        String[] winners = winner.split("-");
        if(winners[0].contains("0")){
            black = true;
        }else if (winners[1].contains("0")){
            white = true;
        }

        for (int i = 0; i < parts.length - 2; i++) {
            pgnMoves.add(parts[i]);
        }

        Board board = Board.createStandardBoard();
        boolean goodData = true;
        Map<Integer, Pair<List<Integer>, Integer>> inputOutputMap = new HashMap<>();

        for (int i = 0; i < pgnMoves.size(); i++) {

            String pgnMove = pgnMoves.get(i);
            Move bestMove = null;
            int bestMoveRank = 0;
            List<Integer> boardState = new ArrayList<>();

            for(int b = 0; b < BoardUtils.NUM_TILES; b++){
                if(board.currentPlayer().isBlack()) {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(-board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }else {
                    if(board.getTile(b).isTileOccupied()) {
                        boardState.add(board.getTile(b).getPiece().getPieceValue());
                    }else {
                        boardState.add(0);
                    }
                }
            }

            for (final Move move : board.currentPlayer().getLegalMoves()) {

                final MoveTransition moveTransition = board.currentPlayer().makeMove(move);

                if (moveTransition.getMoveStatus().isDone()) {

                    if (move.toString().equals(pgnMove)) {
                        bestMove = move;
                        bestMoveRank = Integer.parseInt(BoardUtils.getPositionAtCoordinate(move.getDestinationCoordinate()).substring(1,2));
                    }
                }
            }

            if(bestMove == null){
                System.out.println("Fuck! " + pgnMove + " at move" + i);
                goodData = false;
                break;
            }

            if(white && board.currentPlayer().getAlliance().isWhite()){
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMoveRank);
                inputOutputMap.put(i, inout);
            }else if (black && board.currentPlayer().getAlliance().isBlack()) {
                Pair<List<Integer>, Integer> inout = new Pair<>(boardState, bestMoveRank);
                inputOutputMap.put(i, inout);
            }
            board = board.currentPlayer().makeMove(bestMove).getTransitionBoard();

        }

        if(goodData) {
            File file = new File("./TrainingData/rank.txt");
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);

                for(Map.Entry<Integer, Pair<List<Integer>, Integer>> entry : inputOutputMap.entrySet()){
                    bw.write(entry.getValue().toString() + "\n");
                }
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static int fileToNum(int dest){

        String file = BoardUtils.getPositionAtCoordinate(dest).substring(0,1);

        switch (file){
            case "a":
                return 1;
            case "b":
                return 2;
            case "c":
                return 3;
            case "d":
                return 4;
            case "e":
                return 5;
            case "f":
                return 6;
            case "g":
                return 7;
            case "h":
                return 8;
        }

        return 1;

    }


}
