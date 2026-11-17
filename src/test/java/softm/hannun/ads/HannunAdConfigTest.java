package softm.hannun.ads;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HannunAdConfigTest {
    @Test public void preservesSeparatePlacementIds() {
        HannunAdConfig config = new HannunAdConfig("native", "banner", "interstitial", "exit");
        assertEquals("native", config.nativeUnitId);
        assertEquals("banner", config.adaptiveBannerUnitId);
        assertEquals("interstitial", config.interstitialUnitId);
        assertEquals("exit", config.exitBannerUnitId);
    }

    @Test public void allowsMissingOptionalFallbackBanner() {
        HannunAdConfig config = new HannunAdConfig("native", " ", "interstitial", "exit");
        assertEquals(null, config.adaptiveBannerUnitId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankRequiredIds() {
        new HannunAdConfig(" ", null, "interstitial", "exit");
    }
}
