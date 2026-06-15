package com.bannergress.overlay;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.view.View;

import androidx.preference.PreferenceManager;

import java.util.function.BiConsumer;

public class OverlayService extends Service {
    private OverlayView overlayView;

    /** Text from the original share intent — kept so we can recreate the overlay if needed. */
    private String intentText;

    private boolean notificationControlsEnabled = false;

    private BiConsumer<State, State> stateNotificationListener;
    private BiConsumer<State, State> stateLocationListener;
    private BiConsumer<State, State> stateOverlayVisibilityListener;

    private LocationListener locationListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intentText = intent.getStringExtra(Intent.EXTRA_TEXT);

        stateNotificationListener = StateManager.addListener(this::handleStateNotification);
        stateLocationListener = StateManager.addListener(this::handleStateLocation);
        stateOverlayVisibilityListener = StateManager.addListener(this::handleStateOverlayVisibility);

        addOverlay();
        initPreferences();

        return START_NOT_STICKY;
    }

    private void initPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        applyPreferences(preferences);
        preferences.registerOnSharedPreferenceChangeListener(
                (sharedPreferences, key) -> applyPreferences(sharedPreferences));
    }

    private void applyPreferences(SharedPreferences preferences) {
        StateManager.updateState(State::locationEnabled);

        boolean newNotificationControls = preferences.getBoolean(
                getString(R.string.notification_controls_enable), false);

        if (newNotificationControls != notificationControlsEnabled) {
            notificationControlsEnabled = newNotificationControls;
            ServiceNotification.updateDefaultNotification(this, StateManager.getState(), notificationControlsEnabled);
            updateOverlayVisibility(StateManager.getState());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StateManager.removeListener(stateLocationListener);
        StateManager.removeListener(stateNotificationListener);
        StateManager.removeListener(stateOverlayVisibilityListener);
        removeOverlay();
    }

    private void addOverlay() {
        if (overlayView == null) {
            overlayView = OverlayView.create(this, intentText);
        }
        updateOverlayVisibility(StateManager.getState());
    }

    private void removeOverlay() {
        if (overlayView != null) {
            overlayView.remove();
            overlayView = null;
        }
    }

    /**
     * Show or hide the overlay based on mode and state:
     *
     * - Always hidden when paused (Hide button tapped).
     * - In notification controls mode: visible until the user has actually
     *   launched a mission (hasLaunched). This covers the case where the user
     *   uses +/- to pick a starting mission before tapping Start — the overlay
     *   stays up until Start/Next is tapped and Ingress is launched.
     * - In default mode: always visible (unless paused).
     */
    private void updateOverlayVisibility(State state) {
        if (overlayView == null) return;
        boolean shouldShow = !state.paused && (!notificationControlsEnabled || !state.hasLaunched);
        overlayView.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void handleStateOverlayVisibility(State newState, State oldState) {
        updateOverlayVisibility(newState);
    }

    private void handleStateNotification(State newState, State oldState) {
        ServiceNotification.createNotificationChannels(this);
        ServiceNotification.updateDefaultNotification(this, newState, notificationControlsEnabled);
        ServiceNotification.updateStepReachedNotification(this, newState, oldState);
        ClipboardHandler.updateClipboard(this, newState, oldState);
    }

    private void handleStateLocation(State newState, State oldState) {
        if (newState.locationEnabled) {
            addLocationListening();
        } else {
            removeLocationListening();
        }
    }

    private void addLocationListening() {
        if (locationListener == null
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = getSystemService(LocationManager.class);
            locationListener = location -> StateManager.updateState(state -> state.location(location));
            locationManager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER, 100, 0, locationListener);
        }
    }

    private void removeLocationListening() {
        if (locationListener != null) {
            LocationManager locationManager = getSystemService(LocationManager.class);
            locationManager.removeUpdates(locationListener);
            locationListener = null;
        }
    }
}
