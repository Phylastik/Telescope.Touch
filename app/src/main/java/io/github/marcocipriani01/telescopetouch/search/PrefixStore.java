/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.marcocipriani01.telescopetouch.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given a set of strings such as search terms, this class allows you to search
 * for that string by prefix.
 *
 * @author John Taylor
 */
public class PrefixStore {

    private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>());
    private final TrieNode root = new TrieNode();

    /**
     * Search for any queries matching this prefix.  Note that the prefix is
     * case-independent.
     * <p>
     * TODO(@tcao) refactor this API. Search should return a relevance ranked list.
     */
    public Set<String> queryByPrefix(String prefix) {
        prefix = prefix.toLowerCase();
        TrieNode n = root;
        for (int i = 0; i < prefix.length(); i++) {
            TrieNode c = n.children.get(prefix.charAt(i));
            if (c == null) {
                return EMPTY_SET;
            }
            n = c;
        }
        Set<String> coll = new HashSet<>();
        collect(n, coll);
        return coll;
    }

    private void collect(TrieNode n, Collection<String> coll) {
        coll.addAll(n.results);
        for (TrieNode trieNode : n.children.values()) {
            collect(trieNode, coll);
        }
    }

    /**
     * Put a new string in the store.
     */
    public void add(String string) {
        TrieNode n = root;
        String lower = string.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            TrieNode c = n.children.get(lower.charAt(i));
            if (c == null) {
                c = new TrieNode();
                n.children.put(lower.charAt(i), c);
            }
            n = c;
        }
        n.results.add(string);
    }

    /**
     * Put a whole load of objects in the store at once.
     *
     * @param strings a collection of strings.
     */
    public void addAll(Collection<String> strings) {
        for (String string : strings) {
            add(string);
        }
    }

    static private class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();

        // we need to store the originals to support insensitive case searching
        Set<String> results = new HashSet<>();
    }
}