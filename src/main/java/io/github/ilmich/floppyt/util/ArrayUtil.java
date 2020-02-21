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

public class ArrayUtil {

	// private static final List<String> EMPTY_STRING_LIST = Arrays.asList("");
	// private static final String[] EMPTY_STRING_ARRAY = new String[0];
	public static String[] dropFromEndWhile(String[] array, String regex) {
		for (int i = array.length - 1; i >= 0; i--) {
			if (!array[i].trim().equals("")) {
				String[] trimmedArray = new String[i + 1];
				System.arraycopy(array, 0, trimmedArray, 0, i + 1);
				return trimmedArray;
			}
		}
		return null;
		// { // alternative impl
		// List<String> list = new ArrayList<String>(Arrays.asList(array));
		// list.removeAll(EMPTY_STRING_LIST);
		// return list.toArray(EMPTY_STRING_ARRAY);
		// }
	}

}
