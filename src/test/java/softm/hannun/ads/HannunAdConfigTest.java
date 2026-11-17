package softm.hannun.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test public void disablesPlacementsWithMissingIds() {
        HannunAdConfig config = new HannunAdConfig(" ", null, "", null);
        assertEquals(null, config.nativeUnitId);
        assertEquals(null, config.interstitialUnitId);
        assertEquals(null, config.exitBannerUnitId);
    }

    @Test public void enablesExitOnlyAfterExitAdIsAttached() {
        assertFalse(HannunAds.isExitAllowed(false));
        assertTrue(HannunAds.isExitAllowed(true));
    }
}
