package softm.hannun.ads;

import android.webkit.WebSettings;

/** Shared native-ad marker used by web apps to suppress Kakao AdFit. */
public final class HannunWebViewAds {
    public static final String USER_AGENT_MARKER = "AdMobNative/1";

    private HannunWebViewAds() {}

    public static void markNativeAds(WebSettings settings) {
        String current = settings.getUserAgentString();
        if (current == null) current = "";
        if (!current.contains(USER_AGENT_MARKER)) {
            settings.setUserAgentString((current + " " + USER_AGENT_MARKER).trim());
        }
    }
}
