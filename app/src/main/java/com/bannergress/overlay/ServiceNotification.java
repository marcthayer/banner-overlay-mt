package com.bannergress.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.bannergress.overlay.api.Mission;
import com.bannergress.overlay.api.MissionStep;
import com.bannergress.overlay.api.Objective;
import com.bannergress.overlay.api.POIType;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class ServiceNotification {
    static final String DEFAULT_CHANNEL_ID = "default";
    static final String STEP_IN_RANGE_CHANNEL_ID = "step";

    private static final int DEFAULT_NOTIFICATION_ID = 1;
    private static final int STEP_IN_RANGE_NOTIFICATION_ID_BASE = 2;
    private static final int STEP_IN_RANGE_NOTIFICATION_ID_MULTIPLIER = 100;

    // Request codes for PendingIntents — must be unique
    private static final int RC_STOP   = 0;
    private static final int RC_PAUSE  = 1;
    private static final int RC_RESUME = 2;
    private static final int RC_PREV   = 3;
    private static final int RC_NEXT   = 4;

    static void createNotificationChannels(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);

        NotificationChannel channelDefault = new NotificationChannel(
                DEFAULT_CHANNEL_ID,
                context.getString(R.string.notificationChannelDefaultTitle),
                NotificationManager.IMPORTANCE_DEFAULT);
        channelDefault.setImportance(NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channelDefault);

        NotificationChannel channelStep = new NotificationChannel(
                STEP_IN_RANGE_CHANNEL_ID,
                context.getString(R.string.notificationChannelStepInRangeTitle),
                NotificationManager.IMPORTANCE_DEFAULT);
        channelStep.setImportance(NotificationManager.IMPORTANCE_NONE);
        nm.createNotificationChannel(channelStep);
    }

    static void updateDefaultNotification(Service context, State state, boolean notificationControlsEnabled) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context.getApplicationContext(), DEFAULT_CHANNEL_ID);

        // Content
        if (state.banner == null) {
            builder.setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notificationTitle))
                    .setProgress(0, 0, false);
        } else {
            double totalDistance = DistanceCalculation.getTotalDistance(state.banner);
            double remainingDistance = DistanceCalculation.getRemainingDistance(
                    state.banner, state.currentMission,
                    state.currentMissionVisitedStepIndexes, state.currentLocation);
            builder.setContentTitle(state.banner.title)
                    .setContentText(context.getString(
                            R.string.notificationRemaining,
                            remainingDistance / 1_000,
                            totalDistance / 1_000))
                    .setProgress((int) totalDistance,
                            Math.max((int) (totalDistance - remainingDistance), 0), false);
        }

        // Actions
        if (notificationControlsEnabled) {
            addNotificationControlActions(context, builder, state);
        } else {
            addPauseResumeActions(context, builder, state);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOnlyAlertOnce(true)
                .setAllowSystemGeneratedContextualActions(false)
                .setOngoing(true)
                .build();

        context.startForeground(DEFAULT_NOTIFICATION_ID, notification);
    }

    // --- Default mode: Pause/Resume + Stop ---

    private static void addPauseResumeActions(Service context, NotificationCompat.Builder builder, State state) {
        if (state.paused) {
            builder.addAction(makeAction(context, R.string.notificationActionResume,
                    NotificationActionReceiver.ACTION_RESUME, RC_RESUME));
        } else {
            builder.addAction(makeAction(context, R.string.notificationActionPause,
                    NotificationActionReceiver.ACTION_PAUSE, RC_PAUSE));
        }
        builder.addAction(makeStopAction(context));
    }

    // --- Notification controls mode: Prev / Next (X/Y) / Stop ---

    private static void addNotificationControlActions(Service context, NotificationCompat.Builder builder, State state) {
        if (state.banner != null) {
            int total = state.banner.missions.size();

            // Prev — only shown when there's somewhere to go back to
            if (state.currentMission > 0) {
                builder.addAction(makeAction(context, R.string.notificationActionPrev,
                        NotificationActionReceiver.ACTION_PREV, RC_PREV));
            }

            // Next / Start / Finish
            int displayNext = state.currentMission + 2; // 1-based index of the mission that will be launched
            boolean isLast = (state.currentMission + 1 >= total);

            if (isLast && state.currentMission >= 0) {
                // Banner finished — just stop
                builder.addAction(makeStopAction(context));
                return;
            }

            String nextLabel;
            if (state.currentMission == -1) {
                nextLabel = context.getString(R.string.notificationActionStart,
                        1, total);
            } else {
                nextLabel = context.getString(R.string.notificationActionNext,
                        displayNext, total);
            }

            PendingIntent nextIntent = makeBroadcastIntent(context,
                    NotificationActionReceiver.ACTION_NEXT, RC_NEXT);
            builder.addAction(new NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground, nextLabel, nextIntent).build());
        }

        builder.addAction(makeStopAction(context));
    }

    // --- Helpers ---

    private static NotificationCompat.Action makeAction(Service context, @StringRes int labelRes,
                                                         String broadcastAction, int requestCode) {
        return new NotificationCompat.Action.Builder(
                R.drawable.ic_launcher_foreground,
                context.getString(labelRes),
                makeBroadcastIntent(context, broadcastAction, requestCode)
        ).build();
    }

    private static NotificationCompat.Action makeStopAction(Service context) {
        Intent stopIntent = new Intent(context, StopDetectionReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, RC_STOP, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.notificationAction),
                pi
        ).build();
    }

    private static PendingIntent makeBroadcastIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(action);
        intent.setClass(context, NotificationActionReceiver.class);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void updateStepReachedNotification(Context context, State newState, State oldState) {
        Map<Integer, MissionStep> newStepsInRange = State.getNewStepsInRange(newState, oldState);
        for (Map.Entry<Integer, MissionStep> entry : newStepsInRange.entrySet()) {
            int stepIndex = entry.getKey();
            MissionStep step = entry.getValue();
            if (step.poi != null && step.objective != null && step.poi.type != POIType.unavailable) {
                NotificationManager nm = context.getSystemService(NotificationManager.class);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        context.getApplicationContext(), STEP_IN_RANGE_CHANNEL_ID)
                        .setContentTitle(context.getString(getObjectiveName(step.objective)))
                        .setContentText(step.poi.title)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setAllowSystemGeneratedContextualActions(false)
                        .setTimeoutAfter(8_000);
                Ingress.createLaunchIngressIntent(context).ifPresent(builder::setContentIntent);
                int notificationId = STEP_IN_RANGE_NOTIFICATION_ID_BASE
                        + STEP_IN_RANGE_NOTIFICATION_ID_MULTIPLIER * newState.currentMission
                        + stepIndex;
                nm.notify(notificationId, builder.build());
            }
        }
    }

    private static @StringRes int getObjectiveName(Objective objective) {
        switch (objective) {
            case hack:             return R.string.objectiveHack;
            case captureOrUpgrade: return R.string.objectiveCaptureOrUpgrade;
            case createLink:       return R.string.objectiveCreateLink;
            case createField:      return R.string.objectiveCreateField;
            case installMod:       return R.string.objectiveInstallMod;
            case takePhoto:        return R.string.objectiveTakePhoto;
            case viewWaypoint:     return R.string.objectiveViewWaypoint;
            case enterPassphrase:  return R.string.objectiveEnterPassphrase;
            default: throw new IllegalArgumentException(objective.toString());
        }
    }
}
