/*
 * Copyright (c) 2024 SMS Backup+
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.scheduler;

import androidx.annotation.NonNull;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Minimal Java analog of Kotlin's {@code StateFlow} for use in the
 * {@link BackupScheduler#observe} port method.
 * <p>
 * Holds a current value and notifies registered observers on change. When Kotlin
 * coroutines are added in U-014/U-019, this type will be replaced by a real
 * {@code StateFlow<T>} adapter; the port signature ({@link BackupScheduler#observe})
 * will change to {@code StateFlow<SchedulerState>} at that point.
 * <p>
 * The {@code LegacyScheduler} returns an instance initialised with
 * {@link SchedulerState#Unknown} and never emits further values — this is the
 * expected no-op behavior since Firebase JobDispatcher has no observable state API.
 *
 * @param <T> state type (typically {@link SchedulerState})
 */
public final class SchedulerObservable<T> {

    /** Simple observer callback. */
    public interface Observer<T> {
        void onStateChanged(@NonNull T state);
    }

    private volatile T currentValue;
    private final List<Observer<T>> observers = new CopyOnWriteArrayList<>();

    public SchedulerObservable(@NonNull T initialValue) {
        this.currentValue = initialValue;
    }

    /** Returns the current value without blocking. */
    @NonNull
    public T getValue() {
        return currentValue;
    }

    /** Updates the current value and notifies all registered observers. */
    public void setValue(@NonNull T value) {
        this.currentValue = value;
        for (Observer<T> observer : observers) {
            observer.onStateChanged(value);
        }
    }

    /** Registers an observer. The observer is notified immediately with the current value. */
    public void observe(@NonNull Observer<T> observer) {
        observers.add(observer);
        observer.onStateChanged(currentValue);
    }

    /** Removes a previously registered observer. */
    public void removeObserver(@NonNull Observer<T> observer) {
        observers.remove(observer);
    }
}
