package io.github.marcocipriani01.telescopetouch.search;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Set;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.ApplicationComponent;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApplication;
import io.github.marcocipriani01.telescopetouch.layers.LayerManager;

/**
 * Provides search suggestions for a list of words and their definitions.
 */
public class SearchTermsProvider extends ContentProvider {

    private static final String TAG = TelescopeTouchApplication.getTag(SearchTermsProvider.class);
    private static final int SEARCH_SUGGEST = 0;
    /**
     * The columns we'll include in our search suggestions.
     */
    private static final String[] COLUMNS = {"_id", // must include this column
            SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2};
    public static String AUTHORITY = "io.github.marcocipriani01.telescopetouch.searchterms";
    private static final UriMatcher uriMatcher = buildUriMatcher();
    private static int s = 0;
    @Inject
    LayerManager layerManager;
    private boolean alreadyInjected;

    /**
     * Sets up a uri matcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        maybeInjectMe();
        return true;
    }

    private boolean maybeInjectMe() {
        // Ugh.  Android's separation of content providers from their owning apps makes this
        // almost impossible.  TODO(jontayler): revisit and see if we can make this less
        // nasty.
        if (alreadyInjected) {
            return true;
        }
        Context appContext = getContext().getApplicationContext();
        if (!(appContext instanceof TelescopeTouchApplication)) {
            return false;
        }
        ApplicationComponent component = ((TelescopeTouchApplication) appContext).getApplicationComponent();
        if (component == null) {
            return false;
        }
        component.inject(this);
        alreadyInjected = true;
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "Got query for " + uri);
        if (!maybeInjectMe()) {
            return null;
        }
        if (!TextUtils.isEmpty(selection)) {
            throw new IllegalArgumentException("selection not allowed for " + uri);
        }
        if (selectionArgs != null && selectionArgs.length != 0) {
            throw new IllegalArgumentException("selectionArgs not allowed for " + uri);
        }
        if (!TextUtils.isEmpty(sortOrder)) {
            throw new IllegalArgumentException("sortOrder not allowed for " + uri);
        }
        if (uriMatcher.match(uri) == SEARCH_SUGGEST) {
            String query = null;
            if (uri.getPathSegments().size() > 1) {
                query = uri.getLastPathSegment();
            }
            Log.d(TAG, "Got suggestions query for " + query);
            return getSuggestions(query);
        }
        throw new IllegalArgumentException("Unknown URL " + uri);
    }

    private Cursor getSuggestions(String query) {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        if (query == null) {
            return cursor;
        }
        Set<SearchTerm> results = layerManager.getObjectNamesMatchingPrefix(query);
        Log.d("SearchTermsProvider", "Got results n=" + results.size());
        for (SearchTerm result : results) {
            cursor.addRow(columnValuesOfSuggestion(result));
        }
        return cursor;
    }

    private Object[] columnValuesOfSuggestion(SearchTerm suggestion) {
        return new String[]{Integer.toString(s++), // _id
                suggestion.query, // query
                suggestion.query, // text1
                suggestion.origin, // text2
        };
    }

    /**
     * All queries for this provider are for the search suggestion mime type.
     */
    @Override
    public String getType(Uri uri) {
        if (uriMatcher.match(uri) == SEARCH_SUGGEST) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        throw new IllegalArgumentException("Unknown URL " + uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public static class SearchTerm {
        public String origin;
        public String query;

        public SearchTerm(String query, String origin) {
            this.query = query;
            this.origin = origin;
        }
    }
}