package softm.hannun.ads;

import androidx.annotation.NonNull;

/** App-specific AdMob unit IDs. Every placement must use a separately issued ID. */
public final class HannunAdConfig {
    @NonNull public final String nativeUnitId;
    @NonNull public final String adaptiveBannerUnitId;
    @NonNull public final String interstitialUnitId;
    @NonNull public final String exitBannerUnitId;

    public HannunAdConfig(
            @NonNull String nativeUnitId,
            @NonNull String adaptiveBannerUnitId,
            @NonNull String interstitialUnitId,
            @NonNull String exitBannerUnitId) {
        this.nativeUnitId = requireId(nativeUnitId, "nativeUnitId");
        this.adaptiveBannerUnitId = requireId(adaptiveBannerUnitId, "adaptiveBannerUnitId");
        this.interstitialUnitId = requireId(interstitialUnitId, "interstitialUnitId");
        this.exitBannerUnitId = requireId(exitBannerUnitId, "exitBannerUnitId");
    }

    private static String requireId(String value, String name) {
        if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
