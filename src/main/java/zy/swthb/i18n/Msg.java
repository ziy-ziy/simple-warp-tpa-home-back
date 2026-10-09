package zy.swthb.i18n;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 统一消息构造工具
 * <p>
 * - auto（默认）：返回可翻译组件，由客户端根据自身语言显示
 * - 强制语言：由服务端解析文本后返回字面组件，所有玩家统一显示
 * <p>
 * 用于替代 {@link Component#translatableWithFallback(String, String, Object...)}，
 * 参数中的组件（玩家名、可点击按钮等）在强制语言模式下会被原样保留。
 */
public final class Msg {

    private Msg() {}

    public static MutableComponent tr(String key, String fallback, Object... args) {
        if (Translations.isAuto()) {
            return Component.translatableWithFallback(key, fallback, args);
        }
        return formatLiteral(Translations.get(key, fallback), args);
    }

    /**
     * 将已解析的模板按 %s 占位符与参数拼接为字面组件。
     * 组件类型的参数原样追加，以保留点击/悬停事件与自身样式。
     */
    private static MutableComponent formatLiteral(String template, Object[] args) {
        String[] parts = template.split("%s", -1);
        MutableComponent root = Component.literal(parts.length > 0 ? parts[0] : "");
        for (int i = 1; i < parts.length; i++) {
            int argIndex = i - 1;
            if (argIndex < args.length && args[argIndex] != null) {
                root.append(asComponent(args[argIndex]));
            }
            root.append(Component.literal(parts[i]));
        }
        return root;
    }

    private static Component asComponent(Object arg) {
        if (arg instanceof Component component) return component;
        return Component.literal(String.valueOf(arg));
    }
}
