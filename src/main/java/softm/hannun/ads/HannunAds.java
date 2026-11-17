package softm.hannun.ads;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/** Shared UMP-gated AdMob lifecycle for Hannun Android apps. */
public final class HannunAds {
    static final long INTERSTITIAL_FIRST_DELAY_MS = 75_000L;
    static final long INTERSTITIAL_COOLDOWN_MS = 180_000L;
    static final long EXIT_AD_WAIT_TIMEOUT_MS = 5_000L;
    static final int INTERSTITIAL_MAX_PER_SESSION = 3;

    private final Activity activity;
    private final HannunAdConfig config;
    private final LinearLayout section;
    private final FrameLayout host;
    private final Button privacy;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ConsentInformation consent;
    private NativeAd nativeAd;
    private AdView bottomBanner;
    private AdView exitAd;
    private InterstitialAd interstitialAd;
    private FrameLayout pendingExitSlot;
    private Runnable pendingExitResolved;
    private boolean initialized;
    private boolean destroyed;
    private boolean resumed;
    private boolean interstitialLoading;
    private boolean interstitialScheduled;
    private boolean exitAdReady;
    private int interstitialShownCount;
    private long appStartedAt;
    private long lastInterstitialShownAt;

    public HannunAds(@NonNull Activity activity, @NonNull LinearLayout section, @NonNull HannunAdConfig config) {
        this.activity = activity;
        this.section = section;
        this.config = config;
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.WHITE);
        section.setVisibility(View.GONE);
        privacy = new Button(activity);
        privacy.setText("광고 개인정보 설정");
        privacy.setTextSize(11);
        privacy.setMinHeight(dp(48));
        privacy.setVisibility(View.GONE);
        privacy.setOnClickListener(view -> showPrivacyOptions());
        section.addView(privacy, new LinearLayout.LayoutParams(-2, -2));
        host = new FrameLayout(activity);
        section.addView(host, new LinearLayout.LayoutParams(-1, -2));
    }

    public void start() {
        if (destroyed) return;
        appStartedAt = System.currentTimeMillis();
        consent = UserMessagingPlatform.getConsentInformation(activity);
        consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(), () -> {
            updatePrivacy();
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> {
                updatePrivacy();
                maybeLoad();
            });
            maybeLoad();
        }, error -> {
            updatePrivacy();
            maybeLoad();
        });
    }

    public void onResume() {
        resumed = true;
        scheduleInterstitial();
    }

    public void onPause() {
        resumed = false;
    }

    public void showPrivacyOptions() {
        if (!activityAlive()) return;
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> {
            if (error != null) Toast.makeText(activity, "광고 개인정보 설정을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
            updatePrivacy();
            maybeLoad();
        });
    }

    private void maybeLoad() {
        if (initialized || destroyed || consent == null || !consent.canRequestAds()) return;
        initialized = true;
        MobileAds.initialize(activity, status -> activity.runOnUiThread(() -> {
            if (!activityAlive()) return;
            loadNativeAd();
            loadInterstitialAd();
            loadExitAd();
            scheduleInterstitial();
        }));
    }

    private void loadNativeAd() {
        if (!activityAlive()) return;
        new AdLoader.Builder(activity, config.nativeUnitId)
                .forNativeAd(ad -> {
                    if (!activityAlive()) { ad.destroy(); return; }
                    destroyBottomAd();
                    nativeAd = ad;
                    host.removeAllViews();
                    host.addView(render(ad), new FrameLayout.LayoutParams(-1, -2));
                    updateVisibility(true);
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadAdaptiveBannerFallback();
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder().build())
                .build().loadAd(new AdRequest.Builder().build());
    }

    private void loadAdaptiveBannerFallback() {
        if (!activityAlive() || bottomBanner != null || config.adaptiveBannerUnitId == null) {
            host.removeAllViews();
            updateVisibility(false);
            return;
        }
        host.post(() -> {
            if (!activityAlive() || bottomBanner != null || nativeAd != null) return;
            int widthPx = host.getWidth();
            if (widthPx <= 0) widthPx = activity.getResources().getDisplayMetrics().widthPixels;
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            int widthDp = Math.max(1, Math.round(widthPx / metrics.density));
            AdView banner = new AdView(activity);
            banner.setAdUnitId(config.adaptiveBannerUnitId);
            banner.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp));
            bottomBanner = banner;
            banner.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override public void onAdLoaded() {
                    if (bottomBanner != banner || !activityAlive()) { banner.destroy(); return; }
                    host.removeAllViews();
                    host.addView(banner, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
                    updateVisibility(true);
                }

                @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    if (bottomBanner == banner) bottomBanner = null;
                    banner.destroy();
                    host.removeAllViews();
                    updateVisibility(false);
                }
            });
            banner.loadAd(new AdRequest.Builder().build());
        });
    }

    private void loadInterstitialAd() {
        if (interstitialLoading || interstitialAd != null || !activityAlive()) return;
        interstitialLoading = true;
        InterstitialAd.load(activity, config.interstitialUnitId, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialLoading = false;
                interstitialAd = ad;
                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        lastInterstitialShownAt = System.currentTimeMillis();
                        loadInterstitialAd();
                        scheduleInterstitial();
                    }

                    @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        interstitialAd = null;
                        loadInterstitialAd();
                        scheduleInterstitial();
                    }
                });
                scheduleInterstitial();
            }

            @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitialLoading = false;
                handler.postDelayed(HannunAds.this::loadInterstitialAd, INTERSTITIAL_COOLDOWN_MS);
            }
        });
    }

    private void scheduleInterstitial() {
        if (!initialized || !resumed || interstitialScheduled || interstitialShownCount >= INTERSTITIAL_MAX_PER_SESSION) return;
        long now = System.currentTimeMillis();
        long first = Math.max(0L, appStartedAt + INTERSTITIAL_FIRST_DELAY_MS - now);
        long cooldown = lastInterstitialShownAt == 0L ? 0L : Math.max(0L, lastInterstitialShownAt + INTERSTITIAL_COOLDOWN_MS - now);
        interstitialScheduled = true;
        handler.postDelayed(() -> {
            interstitialScheduled = false;
            if (!activityAlive() || !resumed || interstitialShownCount >= INTERSTITIAL_MAX_PER_SESSION) return;
            InterstitialAd ready = interstitialAd;
            if (ready == null) { loadInterstitialAd(); return; }
            interstitialShownCount++;
            ready.show(activity);
        }, Math.max(first, cooldown));
    }

    private void loadExitAd() {
        if (!activityAlive() || exitAd != null) return;
        AdView ad = new AdView(activity);
        ad.setAdSize(AdSize.MEDIUM_RECTANGLE);
        ad.setAdUnitId(config.exitBannerUnitId);
        exitAd = ad;
        exitAdReady = false;
        ad.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override public void onAdLoaded() {
                if (exitAd != ad || !activityAlive()) { ad.destroy(); return; }
                exitAdReady = true;
                attachPendingExitAd();
            }

            @Override public void onAdFailedToLoad(@NonNull LoadAdError error) {
                if (exitAd == ad) { exitAd = null; exitAdReady = false; }
                ad.destroy();
                if (pendingExitSlot != null) { pendingExitSlot.removeAllViews(); pendingExitSlot.setVisibility(View.GONE); }
                resolvePendingExit();
                handler.postDelayed(HannunAds.this::loadExitAd, INTERSTITIAL_COOLDOWN_MS);
            }
        });
        ad.loadAd(new AdRequest.Builder().build());
    }

    public void showExitConfirmation() {
        showExitConfirmation(activity::finish);
    }

    public void showExitConfirmation(@NonNull Runnable onExit) {
        if (!activityAlive()) return;
        FrameLayout slot = new FrameLayout(activity);
        LinearLayout loading = new LinearLayout(activity);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        ProgressBar progress = new ProgressBar(activity);
        loading.addView(progress, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView text = new TextView(activity);
        text.setText("광고를 불러오는 중입니다.");
        text.setPadding(0, dp(12), 0, 0);
        loading.addView(text, new LinearLayout.LayoutParams(-2, -2));
        slot.addView(loading, new FrameLayout.LayoutParams(-1, dp(250)));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("앱을 종료하시겠습니까?")
                .setMessage("계속 이용하거나 앱을 종료할 수 있습니다.")
                .setView(slot)
                .setNegativeButton("계속 이용", null)
                .setPositiveButton("종료", (d, which) -> onExit.run())
                .create();
        Runnable enableExit = () -> { if (dialog.isShowing()) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); };
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            if (!initialized) { slot.removeAllViews(); slot.setVisibility(View.GONE); enableExit.run(); }
            else { pendingExitSlot = slot; pendingExitResolved = enableExit; if (exitAdReady) attachPendingExitAd(); else loadExitAd(); }
            handler.postDelayed(enableExit, EXIT_AD_WAIT_TIMEOUT_MS);
        });
        dialog.setOnDismissListener(d -> {
            handler.removeCallbacks(enableExit);
            if (pendingExitSlot == slot) { pendingExitSlot = null; pendingExitResolved = null; }
            for (int i = 0; i < slot.getChildCount(); i++) if (slot.getChildAt(i) instanceof AdView) ((AdView) slot.getChildAt(i)).destroy();
            slot.removeAllViews();
            loadExitAd();
        });
        dialog.show();
    }

    private void attachPendingExitAd() {
        if (pendingExitSlot == null || !exitAdReady || exitAd == null) return;
        FrameLayout slot = pendingExitSlot;
        AdView ad = exitAd;
        exitAd = null;
        exitAdReady = false;
        slot.removeAllViews();
        slot.addView(ad, new FrameLayout.LayoutParams(dp(300), dp(250), Gravity.CENTER));
        resolvePendingExit();
    }

    private void resolvePendingExit() {
        Runnable resolved = pendingExitResolved;
        pendingExitSlot = null;
        pendingExitResolved = null;
        if (resolved != null) resolved.run();
    }

    private NativeAdView render(NativeAd ad) {
        NativeAdView view = new NativeAdView(activity);
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(8), dp(14), dp(8));
        TextView title = new TextView(activity);
        title.setText("광고  " + ad.getHeadline());
        title.setTextColor(Color.rgb(27, 43, 41));
        title.setTextSize(14);
        title.setMaxLines(2);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button action = new Button(activity);
        if (ad.getCallToAction() == null) action.setVisibility(View.GONE);
        else { action.setText(ad.getCallToAction()); view.setCallToActionView(action); }
        action.setTextSize(12);
        action.setMinHeight(dp(48));
        row.addView(action, new LinearLayout.LayoutParams(-2, -2));
        view.addView(row, new FrameLayout.LayoutParams(-1, -2));
        view.setHeadlineView(title);
        view.setNativeAd(ad);
        return view;
    }

    private void updatePrivacy() {
        if (destroyed) return;
        boolean required = consent != null && consent.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        privacy.setVisibility(required ? View.VISIBLE : View.GONE);
        updateVisibility(host.getChildCount() > 0);
    }

    private void updateVisibility(boolean hasAd) {
        section.setVisibility(hasAd || privacy.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    private void destroyBottomAd() {
        if (nativeAd != null) nativeAd.destroy();
        nativeAd = null;
        if (bottomBanner != null) bottomBanner.destroy();
        bottomBanner = null;
    }

    private boolean activityAlive() {
        return !destroyed && !activity.isFinishing() && !activity.isDestroyed();
    }

    public void destroy() {
        destroyed = true;
        resumed = false;
        handler.removeCallbacksAndMessages(null);
        destroyBottomAd();
        interstitialAd = null;
        if (exitAd != null) exitAd.destroy();
        exitAd = null;
        pendingExitSlot = null;
        pendingExitResolved = null;
        host.removeAllViews();
        section.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
