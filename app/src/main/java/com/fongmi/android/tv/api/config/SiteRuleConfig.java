package com.fongmi.android.tv.api.config;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.SiteRule;
import com.github.catvod.utils.Prefers;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 资源站规则持久化管理（SharedPreferences，JSON 数组）。
 * 规则按 sortCode 升序，禁用的排在最后。
 */
public final class SiteRuleConfig {

    private static final String PREF_KEY = "site_rules";
    private static final Type LIST_TYPE = new TypeToken<List<SiteRule>>() {}.getType();

    private SiteRuleConfig() {}

    public static synchronized List<SiteRule> load() {
        String json = Prefers.getString(PREF_KEY, "[]");
        try {
            List<SiteRule> rules = App.gson().fromJson(json, LIST_TYPE);
            List<SiteRule> result = rules == null ? new ArrayList<>() : rules;
            result.sort(Comparator.comparing(SiteRule::getSortCode)
                    .thenComparing(r -> r.isEnabled() ? 0 : 1));
            return result;
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }

    public static synchronized void save(List<SiteRule> rules) {
        Prefers.put(PREF_KEY, App.gson().toJson(rules == null ? new ArrayList<>() : rules));
    }

    public static synchronized void add(SiteRule rule) {
        if (rule == null) return;
        List<SiteRule> rules = load();
        rules.add(rule);
        save(rules);
    }

    public static synchronized void update(SiteRule rule) {
        if (rule == null || rule.getId().isEmpty()) return;
        List<SiteRule> rules = load();
        for (int i = 0; i < rules.size(); i++) {
            if (rule.getId().equals(rules.get(i).getId())) {
                rules.set(i, rule);
                break;
            }
        }
        save(rules);
    }

    public static synchronized void delete(String id) {
        if (id == null) return;
        List<SiteRule> rules = load();
        rules.removeIf(r -> id.equals(r.getId()));
        save(rules);
    }

    /** 按 URL 路由查找命中的第一条已启用规则。 */
    public static SiteRule match(String url) {
        for (SiteRule rule : load()) {
            if (rule.isEnabled() && rule.matches(url)) return rule;
        }
        return null;
    }

    /** 全部已启用规则（含站点路由与特征码），供 HLS 清洗编译。 */
    public static List<SiteRule> enabled() {
        List<SiteRule> out = new ArrayList<>();
        for (SiteRule rule : load()) if (rule.isEnabled()) out.add(rule);
        return out;
    }
}
