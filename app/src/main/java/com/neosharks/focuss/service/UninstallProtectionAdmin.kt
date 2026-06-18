package com.neosharks.focuss.service

import android.app.admin.DeviceAdminReceiver

/**
 * Minimal device-admin receiver. Its only purpose is to be an *active* admin:
 * while an app has an active device admin, Android blocks the user from
 * uninstalling it until they first deactivate the admin. That deliberate extra
 * step is "strict mode" — it stops impulsive uninstalls mid-focus-session.
 *
 * It requests no policies beyond existing, so it cannot lock the device, wipe
 * data, or change passwords.
 */
class UninstallProtectionAdmin : DeviceAdminReceiver()
