# Hannun AdMob for Android

한눈 Android 앱의 UMP 동의, 하단 네이티브 광고와 적응형 배너 대체, 시간 기반 전면광고, 종료 확인창 광고 생명주기를 공통으로 제공하는 AAR입니다. 운영 광고 ID는 포함하지 않습니다.

## Requirements

- Android `compileSdk 36`, `minSdk 24`
- Java 17
- Google Mobile Ads `25.5.0`
- Google UMP `4.0.0`

앱은 네이티브, 적응형 배너, 전면, 종료 300×250 지면별로 각각 발급된 ID를 `HannunAdConfig`에 전달해야 합니다. Debug 리소스에는 Google 공식 테스트 ID를 사용합니다.

```java
HannunAdConfig config = new HannunAdConfig(
    getString(R.string.admob_native_ad_unit_id),
    getString(R.string.admob_adaptive_banner_ad_unit_id),
    getString(R.string.admob_interstitial_ad_unit_id),
    getString(R.string.admob_exit_banner_ad_unit_id));
HannunAds ads = new HannunAds(this, findViewById(R.id.adHost), config);
HannunWebViewAds.markNativeAds(webView.getSettings());
ads.start();
```

Activity의 `onResume`, `onPause`, `onDestroy`에서 같은 이름의 메서드를 전달하고, 뒤로가기로 앱을 종료할 때 `showExitConfirmation()`을 호출합니다.

## Verification

```bash
./gradlew test assembleRelease publishReleasePublicationToMavenLocal
```

실제 광고 송출, UMP 지역별 동의, Release ID 및 Play 선언은 소비 앱의 실제 서명 Release에서 별도로 검증해야 합니다.
