package zy.swthb.i18n;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端语言管理器
 * <p>
 * 从模组自带的语言文件（assets/simple-warp-tpa-home-back/lang/&lt;locale&gt;.json）读取翻译，
 * 供服务端在强制语言模式下解析文本使用。
 * <p>
 * - {@link #AUTO}：跟随客户端语言（默认，保持原有行为）
 * - 指定 locale：服务端解析文本，所有玩家统一显示该语言
 * <p>
 * 查找顺序：所选语言 → en_us → 调用方传入的中文兜底文本
 */
public final class Translations {

    private static final Logger LOGGER = LoggerFactory.getLogger("simple-warp-tpa-home-back-i18n");
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final String LANG_PATH = "assets/simple-warp-tpa-home-back/lang/%s.json";

    /** 跟随客户端语言 */
    public static final String AUTO = "auto";

    /** 兜底语言 */
    public static final String FALLBACK_LOCALE = "en_us";

    private static final List<String> AVAILABLE = List.of(
            "ar_sa", "en_us", "es_es", "fr_fr", "hi_in", "ja_jp", "ru_ru", "zh_cn", "zh_hk", "zh_tw");

    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private static String locale = AUTO;
    private static Map<String, String> strings = Map.of();

    private Translations() {}

    /** 模组内置的所有语言代码 */
    public static List<String> availableLocales() {
        return AVAILABLE;
    }

    /** 是否为跟随客户端模式 */
    public static boolean isAuto() {
        return locale == null || AUTO.equalsIgnoreCase(locale);
    }

    /** 当前语言代码 */
    public static String getLocale() {
        return locale;
    }

    /** 语言代码是否受支持（auto 或内置语言） */
    public static boolean isSupported(String locale) {
        return locale != null && (AUTO.equalsIgnoreCase(locale) || AVAILABLE.contains(locale.toLowerCase()));
    }

    /** 切换语言并加载对应语言文件 */
    public static void setLocale(String locale) {
        Translations.locale = (locale == null || locale.isBlank()) ? AUTO : locale.toLowerCase();
        strings = isAuto() ? Map.of() : load(Translations.locale);
    }

    /** 使用当前语言代码重新加载语言文件 */
    public static void reload() {
        setLocale(locale);
    }

    /**
     * 解析翻译文本。auto 模式下返回调用方传入的兜底文本（由客户端负责翻译）。
     * 指定语言时返回所选语言的文本；缺失时回退到 en_us，再缺失则返回兜底文本。
     */
    public static String get(String key, String fallback) {
        if (isAuto()) return fallback;

        String value = strings.get(key);
        if (value != null) return value;

        if (!FALLBACK_LOCALE.equals(locale)) {
            String en = load(FALLBACK_LOCALE).get(key);
            if (en != null) return en;
        }
        return fallback;
    }

    private static Map<String, String> load(String locale) {
        return CACHE.computeIfAbsent(locale, Translations::read);
    }

    private static Map<String, String> read(String locale) {
        String path = String.format(LANG_PATH, locale);
        try (InputStream in = Translations.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                LOGGER.warn("找不到语言文件: {}", path);
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
                return map != null ? map : Map.of();
            }
        } catch (Exception e) {
            LOGGER.error("加载语言文件失败 {}: {}", path, e.getMessage());
            return Map.of();
        }
    }
}
