package com.project.marketplace.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SessionUtils {

    /**
     * 현재 세션에 저장된 모든 속성을 맵으로 반환
     */
    public Map<String, Object> getSessionAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Map<String, Object> sessionMap = new HashMap<>();

        if (session != null) {
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                Object value = session.getAttribute(name);
                sessionMap.put(name, value);
            }
            log.info("세션 ID: {}, 속성 수: {}", session.getId(), sessionMap.size());
        } else {
            log.info("활성화된 세션이 없습니다.");
        }

        return sessionMap;
    }

    /**
     * 세션에서 사용자 ID 가져오기
     */
    public Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }

    /**
     * 세션에서 이메일 가져오기
     */
    public String getEmailFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("email") != null) {
            return (String) session.getAttribute("email");
        }
        return null;
    }

    /**
     * 세션에서 이름 가져오기
     */
    public String getNameFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("name") != null) {
            return (String) session.getAttribute("name");
        }
        return null;
    }
}