/*
MIT License

Copyright (c) 2020 Michele Zuccalà

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.github.ilmich.floppyt.util;

/**
 * The Knuth Morris Pratt string searching algorithm (or KMP algorithm) searches
 * for occurrences of a "word" W within a main "text string" S by employing the
 * observation that when a mismatch occurs, the word itself embodies sufficient
 * information to determine where the next match could begin, thus bypassing
 * re-examination of previously matched characters.
 * 
 * The algorithm was conceived by Donald Knuth and Vaughan Pratt and
 * independently by James H. Morris in 1977, but the three published it jointly.
 * 
 */

public class KnuthMorrisPrattAlgorithm {

	/**
	 * Search for pattern in data, [start, end). Returns -1 if no match is found or
	 * if pattern is of length 0.
	 */
	public static int indexOf(byte[] data, int start, int end, byte[] pattern) {
		if (pattern.length == 0) {
			return -1;
		}
		int[] failure = failure(pattern);

		int j = 0;

		for (int i = 0; i < end; i++) {
			while (j > 0 && pattern[j] != data[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == data[i]) {
				j++;
			}
			if (j == pattern.length) {
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	private static int[] failure(byte[] pattern) {
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (j > 0 && pattern[j] != pattern[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i]) {
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}
}
