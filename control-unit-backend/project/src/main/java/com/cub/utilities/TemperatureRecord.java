package com.cub.utilities;

import java.util.LinkedList;
import java.util.Queue;

public class TemperatureRecord {
    private final int capacity;
    private final Queue<Float> temperatures;
    private float sum;
    private float min;
    private float max;

    public TemperatureRecord(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.temperatures = new LinkedList<>();
        this.sum = 0;
        this.min = Float.MAX_VALUE;
        this.max = Float.MIN_VALUE;
    }

    public void addTemperature(float value) {
        if (temperatures.size() == capacity) {
            float removed = temperatures.poll();
            sum -= removed;
            if (removed == min || removed == max) {
                recalculateMinMax();
            }
        }
        temperatures.offer(value);
        sum += value;
        if (value < min)
            min = value;
        if (value > max)
            max = value;
    }

    private void recalculateMinMax() {
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        for (float temp : temperatures) {
            if (temp < min)
                min = temp;
            if (temp > max)
                max = temp;
        }
    }

    public float getAverage() {
        return temperatures.isEmpty() ? 0 : sum / temperatures.size();
    }

    public float getMin() {
        return temperatures.isEmpty() ? Float.NaN : min;
    }

    public float getMax() {
        return temperatures.isEmpty() ? Float.NaN : max;
    }

    public Float getLastTemperature() {
        if (temperatures.isEmpty()) {
            return null;
        }
        Float last = null;
        for (Float temp : temperatures) {
            last = temp;
        }
        return last;
    }

    @Override
    public String toString() {
        return String.format("Min: %.2f, Max: %.2f, Avg: %.2f", getMin(), getMax(), getAverage());
    }
}
