/*
 * SPDX-FileCopyrightText:  2025 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.foo.bar.VeryTrustfulMNO.MQTT.Handler;

import android.content.Context;

import androidx.work.OneTimeWorkRequest;

import org.json.JSONException;

import java.util.ArrayList;

abstract public class Handler {
    private final String TAG = "Handler";
    abstract public void parsePayload(String payload) throws JSONException;

    public Handler() {
    }
    abstract public ArrayList<OneTimeWorkRequest> getExecutorWorkRequests(Context context);

    abstract public ArrayList<OneTimeWorkRequest> getMonitorWorkRequests(Context context);

    abstract public ArrayList<OneTimeWorkRequest> getToLineProtocolWorkRequests(Context context);

    abstract public ArrayList<OneTimeWorkRequest> getUploadWorkRequests(Context context);

    abstract public void preperareSequence(Context context);

    abstract public void enableSequence();

    abstract public void disableSequence(Context context);
}
