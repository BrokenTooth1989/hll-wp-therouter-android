package com.therouter.plugin.utils;

import com.therouter.plugin.BuildConfig;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassCacheUtils {

    public static final String CACHE_SERVICE_PROVIDE = "serviceProvide.therouter";
    public static final String CACHE_AUTOWIRED = "autowired.therouter";
    public static final String CACHE_ROUTE = "route.therouter";

    public static boolean write(Set<String> set, File file) throws IOException {
        String content = set2String(set);
        if (file.exists()) {
            // 文件已存在时才做"内容无变化"的跳过优化
            String cache = set2String(readToSet(file));
            if (content.equals(cache)) {
                return false;
            }
            file.delete();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        ResourceGroovyMethods.write(file, content);
        return true;
    }

    public static Set<String> readToSet(File file) throws IOException {
        Set<String> set = new HashSet<>();
        if (file.exists()) {
            List<String> list = ResourceGroovyMethods.readLines(file);
            for (String str : list) {
                if (!str.isBlank()) {
                    set.add(str);
                }
            }
        }
        return set;
    }

    public static Map<String, String> readToMap(File file) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (file.exists()) {
            List<String> list = ResourceGroovyMethods.readLines(file);
            for (String str : list) {
                if (!str.isBlank()) {
                    map.put(str, BuildConfig.VERSION);
                }
            }
        }
        return map;
    }

    private static String set2String(Set<String> set) {
        StringBuilder stringBuilder = new StringBuilder();
        ArrayList<String> list = new ArrayList<>(set);
        Collections.sort(list);
        for (String str : list) {
            stringBuilder.append(str).append("\n");
        }
        return stringBuilder.toString();
    }
}
