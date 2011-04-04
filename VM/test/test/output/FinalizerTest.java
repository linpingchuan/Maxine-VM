/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.output;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

public class FinalizerTest implements Comparable<FinalizerTest> {

    static final PrintStream out = System.out;

    static class WeakerString extends WeakReference<String> {
        public WeakerString(String s) {
            super(s);
        }
    }

    static class WeakString extends WeakReference<String> {

        // Tests weak-to-weak reference handling
        final WeakerString weakerRef;

        public WeakString(String s) {
            super(s);
            this.weakerRef = new WeakerString(s + "-weaker");
        }
    }

    final String id;
    WeakString weakString;

    static final TreeSet<String> finalized = new TreeSet<String>();

    @Override
    public int compareTo(FinalizerTest o) {
        return id.compareTo(o.id);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finalized.add(id);
    }


    public FinalizerTest(String id) {
        this.id = id;
        this.weakString = new WeakString(id + "-weakRef");
    }

    @Override
    public String toString() {
        return id;
    }

    public static void main(String[] args) throws InterruptedException {
        test(10);
        while (finalized.size() != 10) {
            System.gc();
            Thread.sleep(1000);
        }

        for (String id : finalized) {
            out.println("finalized " + id);
        }
    }

    private static void test(int num) {
        for (int i = 0; i < num; i++) {
            FinalizerTest t = new FinalizerTest("ref" + i);
            out.println("created " + t);
        }
    }
}
