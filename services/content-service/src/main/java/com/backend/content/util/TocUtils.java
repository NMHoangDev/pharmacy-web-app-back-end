package com.backend.content.util;

import com.backend.content.api.dto.TocItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TocUtils {

    private static final Pattern HEADING_PATTERN = Pattern.compile("<h([23])[^>]*>(.*?)</h\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private TocUtils() {
    }

    public static List<TocItem> generate(String contentJson, String contentHtml, ObjectMapper mapper) {
        List<TocItem> toc = new ArrayList<>();
        if (contentJson != null && !contentJson.isBlank()) {
            try {
                JsonNode root = mapper.readTree(contentJson);
                JsonNode blocks = root.get("blocks");
                if (blocks != null && blocks.isArray()) {
                    int idx = 1;
                    for (JsonNode block : blocks) {
                        String type = block.has("type") ? block.get("type").asText("") : "";
                        if (type.contains("header") || type.contains("heading")) {
                            JsonNode data = block.get("data");
                            String text = data != null && data.has("text") ? data.get("text").asText("") : "";
                            int level = data != null && data.has("level") ? data.get("level").asInt(2) : 2;
                            if (!text.isBlank()) {
                                toc.add(new TocItem("sec-" + idx, stripTags(text), level));
                                idx++;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // fallback to HTML
            }
        }

        if (toc.isEmpty() && contentHtml != null && !contentHtml.isBlank()) {
            Matcher matcher = HEADING_PATTERN.matcher(contentHtml);
            int idx = 1;
            while (matcher.find()) {
                int level = Integer.parseInt(matcher.group(1));
                String text = stripTags(matcher.group(2));
                if (!text.isBlank()) {
                    toc.add(new TocItem("sec-" + idx, text, level));
                    idx++;
                }
            }
        }

        return toc;
    }

    private static String stripTags(String input) {
        return input == null ? "" : input.replaceAll("<[^>]*>", "").trim();
    }
}
