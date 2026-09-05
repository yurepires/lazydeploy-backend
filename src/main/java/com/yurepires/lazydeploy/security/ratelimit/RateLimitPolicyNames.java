package com.yurepires.lazydeploy.security.ratelimit;

public final class RateLimitPolicyNames {

    public static final String LOGIN_IP = "login_ip";
    public static final String LOGIN_IDENTITY = "login_identity";
    public static final String REGISTER_IP = "register_ip";
    public static final String SERVER_SEARCH_USER = "server_search_user";
    public static final String SERVER_SEARCH_IP = "server_search_ip";
    public static final String SUBSCRIPTION_CONFIGURE_USER =
            "subscription_configure_user";
    public static final String SUBSCRIPTION_CONFIGURE_IP =
            "subscription_configure_ip";

    private RateLimitPolicyNames() {
    }
}
