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
    private static final ArrayList<Long> operationTimes = new ArrayList<>();
    
    
    
    public SelfSubtract(final int totalRows, final int numThreads, final boolean showDetails) {
            SelfSubtract.totalRows = totalRows;
            SelfSubtract.numThreads = numThreads;
            SelfSubtract.numRowsPerThread = totalRows/numThreads;
            SelfSubtract.showDetails = showDetails;
    }
    
    
    public static void main(String[] args) {  
        
        // Param 1: Number of Rows
        // Param 2: Number of Threads
        // Param 3: Shows Details for Debugging
        
        SelfSubtract selfsubtract = new SelfSubtract(5000000, 45, false);
        
        long avgOperationTime = doWork();
        
        System.out.println("\n\nAverage Operation Time for All Threads: " + avgOperationTime + " ms");
        
    }

    private static long doWork() {
        
        List<Thread> threadList = new ArrayList<>();
        
        long avgOperationTime = 0;
        
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
        
                
        for(int i = 0; i < numThreads; i++){
            int threadNum = i+1;
            threadList.add(new Thread(new ParallelTask(numRowsPerThread, threadNum), "Thread" + threadNum));
        }
        
        Instant start = Instant.now();
        
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
        
        for (int i = 0; i < operationTimes.size(); i++) {
            avgOperationTime += operationTimes.get(i);
        }
        
        
        return(avgOperationTime/numThreads);
        
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
            Instant queryStartTime = Instant.now();
            Instant operationStartTime = Instant.now();
            Instant operationEndTime = Instant.now();
            
            System.out.println(Thread.currentThread().getName() + " started");
            Connection con;
            int count = 0;
            
            try{
                
                queryStartTime = Instant.now();
                
                con = DriverManager.getConnection("jdbc:mysql://localhost/","root","L0la_Caesar");
                
                int startRow = (threadNum - 1) * numRows;
                
                
                String query = query_base + startRow + ", " + numRows;
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(query);
            
                int currentRow = startRow;
                
                operationStartTime = Instant.now();
                 
                
                while(rs.next()){
                    
                    masterArr[currentRow] -= masterArr[currentRow];
                    currentRow += 1;
                    count += 1;
                }
                
                
                operationEndTime = Instant.now();

            }catch(SQLException ex){
                System.out.println(ex.getMessage());
            } 
            
            Duration totalThreadTime = Duration.between(threadStartTime, Instant.now());
            Duration totalQueryTime = Duration.between(queryStartTime, operationStartTime);
            Duration totalOperationTime = Duration.between(operationStartTime, operationEndTime);
            
            if(showDetails) { 
                System.out.println("\n" + Thread.currentThread().getName().toUpperCase());
                System.out.println("Total Operations: " + count);
                System.out.println("Total Thread Time: " + totalThreadTime.toMillis() + " ms");
                System.out.println("Total Query Time: " + totalQueryTime.toMillis() + " ms");
                System.out.println("Total Operation Time: " + totalOperationTime.toMillis() + " ms");
                System.out.println("Thread Start Time: " + DateTimeFormatter.ofPattern("hh:mm:ss.SSS").format(LocalDateTime.ofInstant(threadStartTime, ZoneOffset.UTC)));
                System.out.println("Operation Start Time: " + DateTimeFormatter.ofPattern("hh:mm:ss.SSS").format(LocalDateTime.ofInstant(operationStartTime, ZoneOffset.UTC)));
                System.out.println("Operation End Time: " + DateTimeFormatter.ofPattern("hh:mm:ss.SSS").format(LocalDateTime.ofInstant(operationEndTime, ZoneOffset.UTC)) + "\n\n"); 
            }
            
            else {
                System.out.println("\n" + Thread.currentThread().getName().toUpperCase());
                System.out.println("Total Operation Time: " + totalOperationTime.toMillis() + " ms");
            }
            
            
            operationTimes.add(totalOperationTime.toMillis());

            
        }
    }
}