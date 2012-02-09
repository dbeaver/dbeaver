package org.jkiss.dbeaver.utils;

import java.util.ArrayList;
import java.util.List;

public class ThreadTest implements Runnable {

    private volatile int m_counter;

    public void run()
    {
        for (int i = 0; i < 100000000; i++) {
            m_counter++;
        }
        System.out.println(m_counter);
    }

    public static void main(String[] args) throws InterruptedException
    {
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 8; i++) {
            ThreadTest test1 = new ThreadTest();
            Thread thread1 = new Thread(test1);
            threads.add(thread1);
            thread1.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Done");
    }
}