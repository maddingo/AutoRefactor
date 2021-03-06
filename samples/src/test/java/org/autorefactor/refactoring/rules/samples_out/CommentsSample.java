/*
 * AutoRefactor - Eclipse plugin to automatically refactor Java code bases.
 *
 * Copyright (C) 2013-2015 Jean-Noël Rouvignac - initial API and implementation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program under LICENSE-GNUGPL.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution under LICENSE-ECLIPSE, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.autorefactor.refactoring.rules.samples_out;

public class CommentsSample implements Runnable {

    /** @NonNull */
    public Object o = new Object();

    public Object f(int i) throws Exception {
        return null;
    }

    /**
     * Uppercase first word.
     */
    private int i;
    /** This must be attached as a javadoc to field 'j'. */
    private int j;
    /** Uppercase. */
    private int k;

    /** Convert to a javadoc. */
    public CommentsSample() {
    }

    /**
     * Convert to a javadoc
     * and correctly indent characters closing the javadoc.
     */
    public void test0() {
    }

    /** Remove javadoc below, current comment will be converted to javadoc. */
    private boolean test1(int j) {
        // Remove comment line just below
        return false;
    }

    // Do not convert this line comment to javadoc
    /**
     * Convert to a javadoc.
     */
    public static void main(String[] args) {
        // remove block comment just below
        // remove comment lines just above and below

        try {
            args[0] = "blue";
        } catch (Exception e) {
        }
    }

    /**
     * This javadoc must have a period added at the end of this sentence.
     */
    public void test2() {
        /*
         * This is a javadoc that must be transformed to a blockcomment
         */
        /*
         * This is a blockcomment that must be kept
         */
        // This is a linecomment that must be kept
    }

    /**
     * Add period at the end of this line.
     * @param i an integer argument
     */
    public int test3(int i) {
        return i;
    }

    /**
      Add period at the end of this line.
      @param i an integer argument
     */
    public int test4(int i) {
        return i;
    }

    /**
     * Do not add period at the end of this
     * line!
     */
    public void test5() {
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
    }

    /**
     * Remove empty line at start and end of this javadoc.
     */
    public void removeEmptyLineAtEndOfJavadoc() {
        /*
         * Remove empty line at start and end of this block comment.
         */
    }
}
