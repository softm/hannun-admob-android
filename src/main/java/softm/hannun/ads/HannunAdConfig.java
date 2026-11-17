package softm.hannun.ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** App-specific AdMob unit IDs. Every placement must use a separately issued ID. */
public final class HannunAdConfig {
    @NonNull public final String nativeUnitId;
    @Nullable public final String adaptiveBannerUnitId;
    @NonNull public final String interstitialUnitId;
    @NonNull public final String exitBannerUnitId;

    public HannunAdConfig(
            @NonNull String nativeUnitId,
            @Nullable String adaptiveBannerUnitId,
            @NonNull String interstitialUnitId,
            @NonNull String exitBannerUnitId) {
        this.nativeUnitId = requireId(nativeUnitId, "nativeUnitId");
        this.adaptiveBannerUnitId = optionalId(adaptiveBannerUnitId);
        this.interstitialUnitId = requireId(interstitialUnitId, "interstitialUnitId");
        this.exitBannerUnitId = requireId(exitBannerUnitId, "exitBannerUnitId");
    }

    private static String requireId(String value, String name) {
        if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String optionalId(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value;
    }
}
