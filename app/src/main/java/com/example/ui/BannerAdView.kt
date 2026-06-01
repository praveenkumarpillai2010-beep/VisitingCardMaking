package com.example.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            Log.d("AdMobDebug", "onAdLoaded: Banner ad loaded successfully for unit: $adUnitId")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            super.onAdFailedToLoad(error)
                            Log.e(
                                "AdMobDebug",
                                "onAdFailedToLoad: Failed to load banner ad for unit: $adUnitId. " +
                                        "Error Code: ${error.code}, " +
                                        "Error Message: ${error.message}, " +
                                        "Domain: ${error.domain}"
                            )
                        }

                        override fun onAdOpened() {
                            super.onAdOpened()
                            Log.d("AdMobDebug", "onAdOpened: Banner ad opened")
                        }

                        override fun onAdClosed() {
                            super.onAdClosed()
                            Log.d("AdMobDebug", "onAdClosed: Banner ad closed")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            Log.d("AdMobDebug", "onAdClicked: Banner ad clicked")
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            Log.d("AdMobDebug", "onAdImpression: Banner ad impression recorded")
                        }
                    }
                    loadAd(
                        AdRequest.Builder().build()
                    )
                }
            }
        )
    }
}
