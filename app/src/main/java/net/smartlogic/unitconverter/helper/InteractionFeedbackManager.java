package net.smartlogic.unitconverter.helper;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;

import androidx.annotation.NonNull;

/** Platform-native, optional feedback for direct UI actions. Never owns a View or Activity. */
public final class InteractionFeedbackManager {
    public enum Type { INPUT, ACTION, CONFIRM, ERROR }

    private InteractionFeedbackManager() { }

    /** Remove the framework's unconditional click sound; the preference owns key sounds. */
    public static void configure(@NonNull View control) {
        control.setSoundEffectsEnabled(false);
    }

    public static void perform(@NonNull View source, @NonNull Type type) {
        if (!source.isEnabled() || !source.isAttachedToWindow()) return;
        Preferences preferences = Preferences.getInstance(source.getContext());
        try {
            if (preferences.isKeyVibrationEnabled()) {
                int effect;
                switch (type) {
                    case INPUT: effect = HapticFeedbackConstants.KEYBOARD_TAP; break;
                    case ACTION: effect = HapticFeedbackConstants.VIRTUAL_KEY; break;
                    case CONFIRM:
                        effect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS;
                        break;
                    default:
                        effect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS;
                }
                if (!source.performHapticFeedback(effect) && effect != HapticFeedbackConstants.VIRTUAL_KEY) {
                    source.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
            }
            if (preferences.isKeySoundsEnabled()) {
                int effect = type == Type.INPUT ? SoundEffectConstants.CLICK
                        : type == Type.ACTION ? SoundEffectConstants.NAVIGATION_LEFT
                        : type == Type.CONFIRM ? SoundEffectConstants.NAVIGATION_DOWN
                        : SoundEffectConstants.NAVIGATION_UP;
                source.getRootView().playSoundEffect(effect);
            }
        } catch (RuntimeException ignored) {
            // OEM feedback services may be absent or restricted; input must still succeed.
        }
    }
}
