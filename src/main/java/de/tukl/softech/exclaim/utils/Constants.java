package de.tukl.softech.exclaim.utils;

import de.tukl.softech.exclaim.transferdata.PreviewFileType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    static {
        Map<String, String> langClassMapping = new HashMap<>();
        langClassMapping.put("java", "lang-java");
        langClassMapping.put("m", "lang-matlab");
        langClassMapping.put("fs", "lang-ml");
        langClassMapping.put("fsi", "lang-ml");
        langClassMapping.put("c", "lang-c");
        langClassMapping.put("h", "lang-c");
        langClassMapping.put("py", "lang-python");
        LANG_CLASS_MAPPING = langClassMapping;

        Map<String, PreviewFileType> extensionTypeMapping = new HashMap<>();
        for (String ext : langClassMapping.keySet()) {
            extensionTypeMapping.put(ext, PreviewFileType.Text);
        }
        extensionTypeMapping.put("fsproj", PreviewFileType.Text);
        extensionTypeMapping.put("txt", PreviewFileType.Text);
        extensionTypeMapping.put("md", PreviewFileType.Text);
        extensionTypeMapping.put("jpg", PreviewFileType.Image);
        extensionTypeMapping.put("jpeg", PreviewFileType.Image);
        extensionTypeMapping.put("png", PreviewFileType.Image);
        extensionTypeMapping.put("pdf", PreviewFileType.PDF);
        EXTENSION_TYPE_MAPPING = extensionTypeMapping;
    }
    public static final String FEEDBACK_SUB = "__feedback";
    public static final String DATA_PATH = "data/";
    public static final DateTimeFormatter DATE_FORMATER = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    public static final long MAX_DISPLAY_FILESIZE = 1024 * 1024; // 1MB
    public static final Map<String, String> LANG_CLASS_MAPPING;
    private static final Map<String, PreviewFileType> EXTENSION_TYPE_MAPPING;

    public static PreviewFileType previewFileTypeByExtension(String extension) {
        return EXTENSION_TYPE_MAPPING.getOrDefault(extension.toLowerCase(), PreviewFileType.NoPreview);
    }
}
