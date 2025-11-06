/*
 * SPDX-FileCopyrightText:  2025 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.foo.bar.VeryTrustfulMNO.Iperf3.JSON.Interval.Streams.UDP;

import de.foo.bar.VeryTrustfulMNO.Iperf3.JSON.Interval.Streams.STREAM_TYPE;
import org.json.JSONException;
import org.json.JSONObject;

public class UDP_UL_STREAM extends UDP_STREAM {
    public UDP_UL_STREAM(){
        super();
    }
    public void parse(JSONObject data) throws JSONException {
        super.parse(data);
        this.setStreamType(STREAM_TYPE.UDP_UL);
    }
}
