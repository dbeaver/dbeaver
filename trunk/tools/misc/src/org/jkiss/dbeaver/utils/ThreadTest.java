/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

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