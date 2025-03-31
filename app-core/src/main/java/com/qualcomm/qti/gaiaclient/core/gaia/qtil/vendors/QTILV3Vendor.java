/*
 * ************************************************************************************************
 * * © 2020-2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.vendors;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTaskManager;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.gaia.core.Plugin;
import com.qualcomm.qti.gaiaclient.core.gaia.core.sending.GaiaSender;
import com.qualcomm.qti.gaiaclient.core.gaia.core.v3.V3Vendor;
import com.qualcomm.qti.gaiaclient.core.gaia.core.v3.packets.V3Packet;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.Feature;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.QTILFeature;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.SupportedFeatures;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.BasicPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.PluginStarter;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.FeaturesFetcher;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.FeaturesFetcherListener;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3ANCPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3AudioCurationPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3BasicPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3BatteryPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3DebugPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3EarbudFitPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3EarbudPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3GestureConfigurationPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3HandsetServicePlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3MusicProcessingPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3QTILPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3QTILPluginError;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3StatisticsPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3UpgradePlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3VoiceProcessingPlugin;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.v3.V3VoiceUIPlugin;
import com.qualcomm.qti.gaiaclient.core.logs.DownloadLogsHelper;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.publishers.QTILFeaturesPublisher;
import com.qualcomm.qti.gaiaclient.core.upgrade.UpgradeHelper;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class QTILV3Vendor extends V3Vendor implements QTILVendor {

    private static final String TAG = "QTILV3Vendor";

    private static final boolean LOG_METHODS = DEBUG.QTIL.QTIL_V3_VENDOR;

    private final UpgradeHelper mUpgradeHelper;

    private final DownloadLogsHelper mDownloadLogsHelper;

    private final FeaturesFetcher mFeaturesFetcher;

    private final QTILFeaturesPublisher mFeaturesPublisher = new QTILFeaturesPublisher();

    @SuppressWarnings("FieldCanBeLocal")
    private final FeaturesFetcherListener mFeaturesListener = new FeaturesFetcherListener() {
        @Override
        public void onSupported(SupportedFeatures features) {
            QTILV3Vendor.this.onSupported(features);
        }

        @Override
        public void onError(Reason reason) {
            QTILV3Vendor.this.onError(reason);
        }
    };

    public QTILV3Vendor(@NonNull PublicationManager publicationManager, @NonNull GaiaSender sender,
                        @NonNull UpgradeHelper upgradeHelper) {
        super(QTILVendorIDs.QTIL_V3_VENDOR_ID, sender);
        this.mUpgradeHelper = upgradeHelper;
        this.mDownloadLogsHelper = new DownloadLogsHelper(publicationManager);
        this.mFeaturesFetcher = new FeaturesFetcher(mFeaturesListener, sender);
        publicationManager.register(mFeaturesPublisher);
    }

    @Override
    public void release() {
        stop();
        this.mDownloadLogsHelper.release();
    }

    @Override
    public Plugin getPlugin(@NonNull QTILFeature feature) {
        return getPlugin(feature.getValue());
    }

    @Override
    protected void onStarted() {
        Logger.log(LOG_METHODS, TAG, "onStarted");
        fetchFeatures();
    }

    @Override
    protected void onStopped() {
        Logger.log(LOG_METHODS, TAG, "onStopped");
        stopAll();
        removeAll();
    }

    @Override
    protected void onNotSupported() {
        Logger.log(LOG_METHODS, TAG, "onNotSupported");
        // nothing to do
    }

    @Override
    protected void onUnhandledPacket(V3Packet packet) {
        if (packet.getFeature() == mFeaturesFetcher.getFeature()) {
            // only happens when the features have not been discovered yet and the vendor has no
            // plugin registered for the BASIC feature yet.
            mFeaturesFetcher.onReceiveGaiaPacket(packet);
        }
        else {
            super.onUnhandledPacket(packet);
        }
    }

    private void onSupported(SupportedFeatures features) {
        mFeaturesFetcher.stop(); // only required to discover the features
        List<QTILFeature> qtilFeatures = onFeaturesDiscovered(features);
        publishUnsupportedFeatures(qtilFeatures);
    }

    private void publishUnsupportedFeatures(List<QTILFeature> supported) {
        for (QTILFeature feature : QTILFeature.getValues()) {
            if (!supported.contains(feature)) {
                mFeaturesPublisher.publishFeatureNotSupported(feature, Reason.NOT_SUPPORTED);
            }
        }
    }

    private void onError(Reason reason) {
        mFeaturesFetcher.stop(); // only required to discover the features
        Log.w(TAG,
              "[DeviceInformationSubscriber->onError] Fetching the supported features resulted in" +
                      " error=" + reason);
        mFeaturesPublisher.publishError(reason);
    }

    private void fetchFeatures() {
        Logger.log(LOG_METHODS, TAG, "fetchFeatures");

        // start the discovery
        mFeaturesFetcher.start();
    }

    private List<QTILFeature> onFeaturesDiscovered(SupportedFeatures supportedFeatures) {
        Logger.log(LOG_METHODS, TAG, "onFeaturesDiscovered", new Pair<>("features",
                                                                        supportedFeatures));

        List<QTILFeature> result = new ArrayList<>();

        List<Feature> features = supportedFeatures.getFeatures();

        // Adding the BasicPlugin first for notification registrations
        Feature basicFeature = supportedFeatures.getFeature(QTILFeature.BASIC.getValue());
        BasicPlugin basicPlugin = basicFeature != null ?
                                  addBasicPlugin(basicFeature.getVersion()) : null;

        if (basicPlugin == null) {
            // unexpected
            Log.w(TAG, "[onFeaturesDiscovered] BASIC feature not provided when fetching the " +
                    "supported features.");
            mFeaturesPublisher.publishError(Reason.NOT_SUPPORTED);
            return result;
        }

        result.add(QTILFeature.BASIC);
        features.remove(basicFeature); // feature managed, removed from the list

        // adding and starting all other plugins
        for (Feature feature : features) {
            QTILFeature qtilFeature = initPlugin(basicPlugin, feature);
            if (qtilFeature != null) {
                result.add(qtilFeature);
            }
        }

        return result;
    }

    @NonNull
    private BasicPlugin addBasicPlugin(int version) {
        Logger.log(LOG_METHODS, TAG, "addBasicPlugin");
        // creating the client
        V3BasicPlugin plugin = new V3BasicPlugin(getSender());
        startPlugin(plugin, plugin, version);
        return plugin;
    }

    private QTILFeature initPlugin(@NonNull BasicPlugin basicPlugin, Feature feature) {
        Logger.log(LOG_METHODS, TAG, "addPlugin", new Pair<>("feature", feature));

        V3QTILPlugin plugin = buildPlugin(feature);
        if (plugin == null) {
            Log.i(TAG,
                  "[initPlugin] QTIL feature not supported by application: feature=" + feature);
            return null;
        }

        startPlugin(basicPlugin, plugin, feature.getVersion());

        return plugin.getQTILFeature();
    }

    private V3QTILPlugin buildPlugin(Feature feature) {
        Logger.log(LOG_METHODS, TAG, "buildPlugin", new Pair<>("feature", feature));

        QTILFeature qtilFeature = QTILFeature.valueOf(feature.getFeatureId());

        if (qtilFeature == null) {
            Log.i(TAG, "[initPlugin] Unknown QTIL feature: feature=" + feature);
            return null;
        }

        switch (qtilFeature) {
            case BASIC:
                return new V3BasicPlugin(getSender());
            case EARBUD:
                return new V3EarbudPlugin(getSender());
            case ANC:
                return new V3ANCPlugin(getSender());
            case AUDIO_CURATION:
                return new V3AudioCurationPlugin((getSender()));
            case VOICE_UI:
                return new V3VoiceUIPlugin(getSender());
            case UPGRADE:
                return new V3UpgradePlugin(getSender(), mUpgradeHelper);
            case DEBUG:
                return new V3DebugPlugin(getSender(), mDownloadLogsHelper);
            case MUSIC_PROCESSING:
                return new V3MusicProcessingPlugin(getSender());
            case EARBUD_FIT:
                return new V3EarbudFitPlugin(getSender());
            case HANDSET_SERVICE:
                return new V3HandsetServicePlugin(getSender());
            case VOICE_PROCESSING:
                return new V3VoiceProcessingPlugin((getSender()));
            case GESTURE_CONFIGURATION:
                return new V3GestureConfigurationPlugin(getSender());
            case BATTERY:
                return new V3BatteryPlugin(getSender());
            case STATISTICS:
                return new V3StatisticsPlugin(getSender());
            default:
                return null;
        }
    }

    private void startPlugin(@NonNull BasicPlugin basicPlugin, @NonNull V3QTILPlugin plugin,
                             int version) {
        // registering the plugin
        addPlugin(plugin);
        // starting the plugin
        plugin.start(version);
        // signing up for notifications
        basicPlugin.registerNotification(plugin.getQTILFeature(),
                                         new PluginStarter(() -> {
                                             if (plugin.getQTILFeature() == QTILFeature.UPGRADE) {
                                                 // upgrade ready to restart if ongoing
                                                 // delaying the start by 1s to let GAIA
                                                 // initialisation process to end
                                                 getTaskManager().schedule(((V3UpgradePlugin) plugin)::onPlugged, 1000);
                                             }
                                             mFeaturesPublisher
                                                     .publishFeatureSupported(plugin.getQTILFeature(), version);
                                         },
                                                           reason -> onNotificationRegistrationFailed(plugin, reason)));
    }

    private void onNotificationRegistrationFailed(V3QTILPlugin plugin, Reason reason) {
        Log.w(TAG, String.format("[onNotificationRegistrationFailed] failed for %1$s, with " +
                                         "reason=%2$s", plugin.getQTILFeature(), reason));

        // stopping the plugin
        if (plugin.onError(V3QTILPluginError.NOTIFICATION_REGISTRATION_FAILED)) {
            removePlugin(plugin.getFeature());
            mFeaturesPublisher.publishFeatureNotSupported(plugin.getQTILFeature(),
                                                          Reason.NOTIFICATION_NOT_SUPPORTED);
        }
    }

    @VisibleForTesting
    List<QTILFeature> getQTILFeatures(SupportedFeatures features) {
        ArrayList<QTILFeature> result = new ArrayList<>();
        for (Feature feature : features.getFeatures()) {
            QTILFeature qtilFeature = QTILFeature.valueOf(feature.getFeatureId());
            if (qtilFeature != null) {
                result.add(qtilFeature);
            }
        }

        return result;
    }

}
