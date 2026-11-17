package softm.hannun.ads;

import androidx.annotation.Nullable;

/** App-specific AdMob unit IDs. Every placement must use a separately issued ID. */
public final class HannunAdConfig {
    @Nullable public final String nativeUnitId;
    @Nullable public final String adaptiveBannerUnitId;
    @Nullable public final String interstitialUnitId;
    @Nullable public final String exitBannerUnitId;

    public HannunAdConfig(
            @Nullable String nativeUnitId,
            @Nullable String adaptiveBannerUnitId,
            @Nullable String interstitialUnitId,
            @Nullable String exitBannerUnitId) {
        this.nativeUnitId = optionalId(nativeUnitId);
        this.adaptiveBannerUnitId = optionalId(adaptiveBannerUnitId);
        this.interstitialUnitId = optionalId(interstitialUnitId);
        this.exitBannerUnitId = optionalId(exitBannerUnitId);
    }

    private static String optionalId(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value;
    }
}
