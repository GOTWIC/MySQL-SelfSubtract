package com.mycompany.mavenproject1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class SelfSubtract {
    
    
    private static final String query_base = "select L_ORDERKEY from tpch.LINEITEM_NEW LIMIT ";
    private static int totalRows;
    private static int numThreads;
    private static int numRowsPerThread;   
    private static int masterArr[] = new int[10000000];
    private static boolean showDetails = false;
    
    
    
    public SelfSubtract(final int totalRows, final int numThreads, final boolean showDetails) {
            SelfSubtract.totalRows = totalRows;
            SelfSubtract.numThreads = numThreads;
            SelfSubtract.numRowsPerThread = totalRows/numThreads;
            SelfSubtract.showDetails = showDetails;
    }
    
    
    public static void main(String[] args) {  
        
        // Param 1 : Number of Rows
        // Param 2: Number of Threads
        // Param 3: Shows Details for Debugging
        
        SelfSubtract selfsubtract = new SelfSubtract(10000000,2, true);
        
        
        long myTime = doWork();  
        
        System.out.println(myTime);
    }

    private static long doWork() {
        
        Instant queryStartTime = Instant.now();
        
        List<Thread> threadList = new ArrayList<>();
        
        try{
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost/","root","L0la_Caesar");
            String query = query_base + "0, " + totalRows;
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            for(int i = 0; i < totalRows; i++){
                
                rs.next();
               
                masterArr[i] = rs.getInt("L_ORDERKEY");
                
            }   
        }catch(SQLException ex){
                System.out.println(ex.getMessage());
        }
        
            
        Instant threadPreWorkStartTime = Instant.now();
        
        for(int i = 0; i < numThreads; i++){
            int threadNum = i+1;
            threadList.add(new Thread(new ParallelTask(numRowsPerThread, threadNum), "Thread" + threadNum));
        }
        
        
        for(int i = 0; i < numThreads; i++){
            threadList.get(i).start();
        }  
        
        for (Thread thread : threadList) {
            try{
                thread.join();
            }catch(InterruptedException ex){
                System.out.println(ex.getMessage());
            }
        }
                
        Duration queryTime = Duration.between(queryStartTime, threadPreWorkStartTime);
        Duration threadPreWorkTime = Duration.between(threadPreWorkStartTime, Instant.now());
        
        System.out.println("\n\n\nTotal Query Time: " + queryTime + "\nTotal Thread PreWork Time: " + threadPreWorkTime + "\n"); 
        
        return(queryTime.toMillis());
        
    }
    
  
    private static class ParallelTask implements Runnable {
        
        private final int numRows;
        private final int threadNum;

        public ParallelTask(final int numRows, final int threadNum) {
            this.numRows = numRows;
            this.threadNum = threadNum;
        }
        
        @Override
        public void run() {
            
            Instant threadStartTime = Instant.now();

            
            System.out.println(Thread.currentThread().getName() + " started");

            int operations = 0;
            
            int startRow = (threadNum - 1) * numRows;
            int currentRow = startRow;          

            for(int i = startRow; i < startRow + numRows; i++){
                masterArr[currentRow] -= masterArr[currentRow];
                currentRow += 1;
                operations += 1;
            }
            
            
            Duration totalThreadTime = Duration.between(threadStartTime, Instant.now());
            
            if(showDetails) { 
                System.out.println("\n\n" + Thread.currentThread().getName().toUpperCase() + "\n" + "Total Operations: " + operations + "\n" + "Total Thread Time: " + totalThreadTime.toMillis() + " ms");
            }
            
        }
    }
}


      