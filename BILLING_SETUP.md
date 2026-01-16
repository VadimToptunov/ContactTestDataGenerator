# Google Play Billing Setup Guide

This guide explains how to set up in-app purchases for the Contact Generator app.

## 📋 Prerequisites

1. **Google Play Console Account** - You need a Google Play Developer account ($25 one-time fee)
2. **App Published** - The app must be published (even as Internal Test Track)
3. **Signed APK/AAB** - You need a release build signed with your keystore

## 🔧 Step-by-Step Setup

### 1. Create In-App Product in Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app: **Contact Generator**
3. Navigate to: **Monetize** → **In-app products**
4. Click **Create product**
5. Fill in the details:
   - **Product ID**: `premium_unlock` (⚠️ MUST match the ID in code)
   - **Name**: Premium Unlock
   - **Description**: Unlock premium features: generate up to 10,000 contacts, batch processing, and more
   - **Status**: Active
   - **Price**: Set your price (e.g., $2.99)
6. Click **Save**

### 2. Upload Your App

You need to upload at least one version to Internal Testing:

```bash
# Build release APK/AAB
cd /Users/vadimtoptunov/AndroidStudioProjects/ContactTestDataGenerator
./gradlew bundleRelease

# The AAB will be at:
# app/build/outputs/bundle/release/app-release.aab
```

1. Go to **Testing** → **Internal testing**
2. Create a new release
3. Upload the **app-release.aab** file
4. Save and review the release

### 3. Add Test Users

For testing purchases without real money:

1. Go to **Settings** → **License testing**
2. Add your Gmail accounts under **License testers**
3. Set response: **License Test Response** → **RESPOND_NORMALLY**
4. Save changes

### 4. Install and Test

#### Option A: Internal Testing Track

1. Accept the internal test invitation email
2. Download the app from Play Store (test track)
3. The billing will work with test mode (no real charges)

#### Option B: Debug Build with Test Accounts

1. Install debug APK on device
2. Make sure you're signed in with a test account in Play Store
3. Purchases will be in test mode (sandbox)

## 🧪 Testing Purchases

### Check Logs

The app now includes detailed logging for billing:

```bash
# View billing logs in Logcat
adb logcat -s BillingManager
```

You should see:
```
D/BillingManager: Billing setup finished: 0 - 
D/BillingManager: Billing connected successfully
D/BillingManager: Querying purchases...
D/BillingManager: Found 0 purchases
D/BillingManager: Launching purchase flow...
D/BillingManager: Querying product details for: premium_unlock
D/BillingManager: Product found: Premium Unlock, price: $2.99
```

### Test Scenarios

1. **Test Purchase Flow**:
   - Tap the star icon (⭐) in the top bar
   - Should open Google Play billing dialog
   - Complete purchase (test mode - no charge)
   - Should see "Premium unlocked! 🎉" toast

2. **Test Premium Features**:
   - Max contacts should increase to 10,000
   - PRO badge should appear in title bar
   - Star icon should disappear

3. **Test Purchase Restoration**:
   - Close and reopen the app
   - Premium status should persist
   - Check logs for "Found 1 purchases"

## ⚠️ Common Issues

### Issue 1: "Product not found"

**Cause**: Product ID mismatch or product not active

**Solution**:
- Verify Product ID in Play Console is exactly: `premium_unlock`
- Check product status is **Active**
- Wait 24 hours after creating product (propagation delay)

### Issue 2: "Billing not ready"

**Cause**: Billing client failed to connect

**Solutions**:
- Check internet connection
- Verify Play Store app is up to date
- Check Logcat for error messages
- Try reinstalling Google Play Store updates

### Issue 3: "Item already owned"

**Cause**: You already purchased in test mode

**Solution**:
- Go to Play Store → Account → Payments & subscriptions → Budget & history
- Find the test purchase and cancel/refund it
- Or use **License testing** → **RESPOND_NORMALLY** for automatic refunds

### Issue 4: Debug build says "Product not found"

**Cause**: Need to test with signed build or Internal Test Track

**Solutions**:
1. Use Internal Testing track (recommended)
2. Or generate a signed debug build:
   ```kotlin
   // In app/build.gradle.kts
   buildTypes {
       debug {
           signingConfig = signingConfigs.getByName("release")
       }
   }
   ```

## 📱 Product Configuration

Current configuration in `BillingManager.kt`:

```kotlin
companion object {
    const val PREMIUM_PRODUCT_ID = "premium_unlock"  // ⚠️ Must match Play Console
    const val FREE_MAX_CONTACTS = 1000
    const val PREMIUM_MAX_CONTACTS = 10000
}
```

## 🔍 Debugging Tips

1. **Enable detailed logging**:
   ```bash
   adb logcat -s BillingManager:V *:S
   ```

2. **Check billing state**:
   - Look for "Billing connected successfully"
   - Check for product query results
   - Monitor purchase updates

3. **Test on real device**:
   - Billing API doesn't work on emulators without Google Play
   - Use physical device with Play Store

4. **Clear app data between tests**:
   ```bash
   adb shell pm clear com.vadimtoptunov.contacttestdatagenerator
   ```

## 📚 Resources

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Test In-App Purchases](https://developer.android.com/google/play/billing/test)
- [Billing Library Integration](https://developer.android.com/google/play/billing/integrate)

## 🎯 Quick Checklist

Before publishing:

- [ ] Product `premium_unlock` created and **Active** in Play Console
- [ ] Price is set for all countries
- [ ] App uploaded to at least Internal Testing track
- [ ] Tested with test account (no real charges)
- [ ] Verified premium features unlock correctly
- [ ] Checked that purchases persist after app restart
- [ ] Tested on multiple devices/Android versions
- [ ] Verified cancellation/refund flow works

---

**Need help?** Check Logcat with `adb logcat -s BillingManager` for detailed error messages!
