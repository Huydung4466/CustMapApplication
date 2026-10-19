package com.huydung.custmapapplication.kmz.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trang Bản đồ (nút 2 trên thanh điều hướng): nhúng Google My Maps chứa toàn bộ điểm KMZ.
 * Cấu hình bằng app.map.my-maps-id trong application.properties (ID hoặc cả đường link My Maps đều được).
 */
@Controller
public class KmzPageController {

    private static final Pattern MID_IN_URL = Pattern.compile("[?&]mid=([A-Za-z0-9_\\-]+)");
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_\\-]{10,100}");

    private final String myMapsId;

    public KmzPageController(@Value("${app.map.my-maps-id:}") String configured) {
        this.myMapsId = extractId(configured);
    }

    @GetMapping("/map")
    public String mapPage(Model model) {
        // Chỉ nhận ký tự an toàn nên có thể đưa thẳng vào đường link trong JSP
        model.addAttribute("myMapsId", myMapsId);
        return "map";
    }

    private static String extractId(String configured) {
        String v = configured == null ? "" : configured.trim();
        Matcher m = MID_IN_URL.matcher(v);
        if (m.find()) {
            v = m.group(1);
        }
        return VALID_ID.matcher(v).matches() ? v : "";
    }
}
