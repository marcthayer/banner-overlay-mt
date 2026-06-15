package com.bannergress.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.bannergress.overlay.api.Mission;

import java.util.Optional;

public class NotificationActionReceiver extends BroadcastReceiver {
    static final String ACTION_PAUSE = "com.bannergress.overlay.ACTION_PAUSE";
    static final String ACTION_RESUME = "com.bannergress.overlay.ACTION_RESUME";
    static final String ACTION_PREV = "com.bannergress.overlay.ACTION_PREV";
    static final String ACTION_NEXT = "com.bannergress.overlay.ACTION_NEXT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PAUSE:
                StateManager.updateState(State::pause);
                break;

            case ACTION_RESUME:
                StateManager.updateState(State::resume);
                break;

            case ACTION_PREV:
                StateManager.updateState(State::previousMission);
                break;

            case ACTION_NEXT: {
                State current = StateManager.getState();
                if (current.banner == null) break;

                int nextIndex = current.currentMission + 1;
                Optional<Mission> nextMission = current.banner.missions.values().stream()
                        .skip(nextIndex)
                        .findFirst();

                if (nextMission.isPresent()) {
                    Ingress.tryLaunchMission(context, nextMission.get().id);
                    StateManager.updateState(state -> state.nextMission(true));
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> StateManager.updateState(State::cooldownFinished),
                            OverlayView.COOLDOWN_MILLIS
                    );
                } else {
                    // Reached end of banner — stop the service
                    Intent serviceIntent = new Intent(context, OverlayService.class);
                    context.stopService(serviceIntent);
                }
                break;
            }
        }
    }
}
