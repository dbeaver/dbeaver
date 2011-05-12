package org.jkiss.dbeaver.utils;

public class ThreadTest implements Runnable {

    private volatile int m_counter;

    public void run()
    {
        for (int i = 0; i < 100000000; i++)
            m_counter++;
    }

    public static void main(String[] args) throws InterruptedException
    {
        ThreadTest test1 = new ThreadTest();
        ThreadTest test2 = new ThreadTest();
        Thread thread1 = new Thread(test1);
        Thread thread2 = new Thread(test2);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        if (test1.m_counter == test2.m_counter)
            System.out.println("Don't care, this is just to fool the optimiser");
    }
}