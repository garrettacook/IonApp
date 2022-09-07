package me.garrett.ionapp;

import android.net.Uri;

import net.openid.appauth.AuthorizationServiceConfiguration;

public final class Ion {
    private Ion() {
    }

    public static final String
            BASE_URL = "https://ion.tjhsst.edu/",
            CLIENT_ID = "kWQhxJDH8KZkd6gIiD7qI40KQhHl7pXngxvSPBy0",
            SCOPE = "read",
            AUTHORIZATION_ENDPOINT = BASE_URL + "oauth/authorize",
            TOKEN_ENDPOINT = BASE_URL + "oauth/token",
            API_ROOT = BASE_URL + "api/";

    public static final AuthorizationServiceConfiguration SERVICE_CONFIG =
            new AuthorizationServiceConfiguration(
                    Uri.parse(AUTHORIZATION_ENDPOINT),
                    Uri.parse(TOKEN_ENDPOINT));

}
