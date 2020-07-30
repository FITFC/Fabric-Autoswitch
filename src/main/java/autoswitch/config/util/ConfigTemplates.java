package autoswitch.config.util;

import autoswitch.AutoSwitch;
import com.google.common.base.Strings;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Enumeration;

public class ConfigTemplates {
    static int width = 131;
    static String commentChar = "#";
    public static String border = Strings.repeat(commentChar, width);
    static String baseCommentedText = "# <FILL> #";
    static String modCategoryDescription = "Config Entries Provided by: ";
    static String defaultValueComment = "\n# Default Value: ";

    public static String modCategory(String modName) {
        return category(modName, modCategoryDescription);

    }

    public static String category(String name, String categoryDesc, String... info) {
        String title = categoryDesc + name;

        if (title.length() < width - 4) {
            title = centeredString(title);
            title = title + Strings.repeat(" ", width - title.length() - 4);
        }

        ArrayList<String> out;

        if (info != null) {
            //title += "\n";
            out = new ArrayList<>();
            out.add(border);
            out.add(baseCommentedText.replace("<FILL>", title));
            for (String s : info) {
                if (s.length() < width - 4) {
                    s = centeredString(s);
                    s = s + Strings.repeat(" ", width - s.length() - 4);
                    out.add(baseCommentedText.replace("<FILL>", s));
                }
            }
            out.add(border);

            String[] a = new String[out.size()];

            return combineToBlock(out.toArray(a));
        }

        return combineToBlock(border, baseCommentedText.replace("<FILL>", title), border);

    }

    public static String configEntry(String key, String value, String comment, String defaultValue) {
        return configEntry(configValueEntry(key, value), comment, defaultValue);
    }

    public static String toolGroupings() {
        StringBuilder keys = new StringBuilder();
        Enumeration<String> enumKeys = AutoSwitch.data.toolGroupings.keys();

        while (enumKeys.hasMoreElements()) {
            keys.append(keys.length() > 0 ? ", " : "").append(enumKeys.nextElement());
        }

        return category("Provided Tool Groupings by AutoSwitch and Mods it Interfaced With", "",
                "Tool groupings are a way to specify multiple tools at once. The names should be self explanatory."
                , keys.toString());
    }

    static String configCommentEntry(String defaultValue) {
        return (defaultValue != null && !defaultValue.equals("")) ? defaultValueComment + defaultValue : "";
    }

    static String configEntry(String cfg, String comment, String defaultValue) {
        StringBuilder out = new StringBuilder();
        if (comment != null && !comment.equals("")) {
            out.append("\n");
            out.append(WordUtils.wrap(comment, width - 2, "\n# ", false));
        }

        out.append(configCommentEntry(defaultValue));

        return combineToBlock(out.toString(), cfg);

    }

    static String configValueEntry(String key, String value) {
        if (value == null) value = "";
        key = key.replaceAll("(?<!\\\\)(?:\\\\{2})*:", "\\:");
        return key + " = " + value;
    }

    static String centeredString(String input) {
        int centering = (int) Math.ceil((width - input.length() - 4) / 2f);

        return Strings.repeat(" ", centering) + input;
    }

    static String combineToBlock(String... strings) {
        StringBuilder out = new StringBuilder();

        for (String string : strings) {
            out.append(string).append("\n");
        }

        out.delete(out.length() - 1, out.length());

        return out.toString();
    }
}
