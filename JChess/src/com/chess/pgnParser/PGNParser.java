package com.chess.pgnParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PGNParser {

    public static void main(String[] args) throws Exception{

        //getPGNString();

    }

    public static List<String> getPGNString(String url) throws Exception{

        List<String> pgnStrings = new ArrayList<>();

        File file = new File(url);
        Scanner input = new Scanner(file);
        StringBuilder sb = new StringBuilder();

        while(input.hasNext()) {

            String nextLine = input.nextLine();

//            if(nextLine.matches(".*\\d+\\..*") && !nextLine.contains("Date")){
//                sb.append(nextLine + "\n");
//                //System.out.println(nextLine);
//            }

            if(!nextLine.contains("[") && !nextLine.contains("]") && !nextLine.contains("Date") && !nextLine.equals("")){
                sb.append(nextLine + "\n");
                //System.out.println(nextLine);
            }


            if((nextLine.contains("1-0") || nextLine.contains("0-1") || nextLine.contains("1/2-1/2")) && !nextLine.contains("Result")){
                pgnStrings.add(sb.toString());
                sb = new StringBuilder();
            }

            //pgnStrings.add(sb.toString());
        }

        input.close();

        return pgnStrings;

    }


    public void saveNetwork(String file) throws Exception{
        File f = new File(file);
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(this);
        out.flush();
        out.close();
    }


}
