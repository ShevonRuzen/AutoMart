package com.shehan.automart.helper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavouriteManager {

    private static final String PREF_NAME = "favourite_pref";
    private static final String KEY_FAVOURITES = "favourite_product";


    private static SharedPreferences getSharedPrefs(Context context){
        return context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
    }

    public static Set<String> getFavs(Context context){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        return new HashSet<>(sharedPrefs.getStringSet(KEY_FAVOURITES,new HashSet<>()));
    }

    public static void addFavs(Context context, String productId){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        HashSet<String> favs = new HashSet<>(sharedPrefs.getStringSet(KEY_FAVOURITES, new HashSet<>()));
        favs.add(productId);
        sharedPrefs.edit().putStringSet(KEY_FAVOURITES,favs).apply();
    }

    public static void removeFav(Context context, String productId){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        HashSet<String> favs = new HashSet<>(sharedPrefs.getStringSet(KEY_FAVOURITES, new HashSet<>()));
        favs.remove(productId);
        sharedPrefs.edit().putStringSet(KEY_FAVOURITES,favs).apply();
    }

    public static boolean isFav(Context context, String productId){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        HashSet<String> favs = new HashSet<>(sharedPrefs.getStringSet(KEY_FAVOURITES, new HashSet<>()));
        return favs.contains(productId);
    }

    public static boolean toggleFav(Context context, String productId){
        SharedPreferences sharedPrefs = getSharedPrefs(context);
        HashSet<String> favs = new HashSet<>(sharedPrefs.getStringSet(KEY_FAVOURITES, new HashSet<>()));

        boolean isNowFav;
        if (favs.contains(productId)){
            favs.remove(productId);
            isNowFav = false;
        }else {
            favs.add(productId);
            isNowFav = true;
        }
        sharedPrefs.edit().putStringSet(KEY_FAVOURITES,favs).apply();
        return isNowFav;
    }
}