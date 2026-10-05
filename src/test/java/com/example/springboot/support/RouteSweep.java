package com.example.springboot.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Test helper: lists every /api/** route registered in a RequestMappingHandlerMapping. */
public final class RouteSweep {

    public record Route(RequestMethod method, String pattern) {}

    private RouteSweep() {}

    public static List<Route> allApiRoutes(RequestMappingHandlerMapping mapping) {
        List<Route> routes = new ArrayList<>();
        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null) {
                continue;
            }
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                if (methods.isEmpty()) {
                    routes.add(new Route(RequestMethod.GET, pattern));
                } else {
                    for (RequestMethod m : methods) {
                        routes.add(new Route(m, pattern));
                    }
                }
            }
        }
        return routes;
    }

    /** Replaces every {placeholder} with 1. */
    public static String fill(String pattern) {
        return pattern.replaceAll("\\{[^}]*\\}", "1");
    }
}
