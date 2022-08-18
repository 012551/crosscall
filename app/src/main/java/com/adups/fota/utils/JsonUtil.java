package com.adups.fota.utils;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static Gson gson;

    private JsonUtil() {
    }

    private static Gson getGson() {
        if (gson == null)
            gson = new Gson();
        return gson;
    }


    public static <T> T jsonObj(String jsonData, Class<T> cls) {
        if (TextUtils.isEmpty(jsonData))
            return null;
        T t;
        try {
            t = getGson().fromJson(jsonData, cls);
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
            return null;
        }
        return t;
    }

    /**
     * 保存json的List对象
     *
     * @param lists
     */
    public static <T> String listToJson(List<T> lists) {
        return getGson().toJson(lists);
    }

    public static <T> String toJson(T object) {
        return getGson().toJson(object);
    }

    public static String map2Json(Map map) {
        return getGson().toJson(map);
    }

    public static Map json2Map(String json) {
        return getGson().fromJson(json, Map.class);
    }

    public static <T> List<T> json2List(String json, Type type) {
        return getGson().fromJson(json, type);
    }

    public static <T> T json2Object(String json) {
        return getGson().fromJson(json, new TypeToken<T>() {
        }.getType());
    }

}
