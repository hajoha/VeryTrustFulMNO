/*
 *  SPDX-FileCopyrightText: 2023 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 *  SPDX-FileCopyrightText: 2023 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.foo.bar.VeryTrustfulMNO.Iperf3.Database.Converter;

import androidx.room.ProvidedTypeConverter;
import androidx.room.TypeConverter;

import com.google.gson.Gson;

import de.foo.bar.VeryTrustfulMNO.Metric.MetricCalculator;


@ProvidedTypeConverter
public class MetricConverter {
    private double median;
    private double mean;
    private double max;
    private double min;
    private double last;
    @TypeConverter
    public MetricCalculator fromJSONString(String string) {
        return new Gson().fromJson(string, MetricCalculator.class);
    }

    @TypeConverter
    public String toJSONString(MetricCalculator example) {
        return new Gson().toJson(example);
    }
}
