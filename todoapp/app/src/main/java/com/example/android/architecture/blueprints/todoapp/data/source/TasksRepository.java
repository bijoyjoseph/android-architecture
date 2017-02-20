/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider;

import java.util.List;

import rx.Completable;
import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 * <p/>
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
public class TasksRepository implements TasksDataSource {

    @Nullable
    private static TasksRepository INSTANCE = null;

    @NonNull
    private final TasksDataSource mTasksRemoteDataSource;

    @NonNull
    private final TasksDataSource mTasksLocalDataSource;

    @NonNull
    private final BaseSchedulerProvider mBaseSchedulerProvider;

    // Prevent direct instantiation.
    private TasksRepository(@NonNull TasksDataSource tasksRemoteDataSource,
                            @NonNull TasksDataSource tasksLocalDataSource,
                            @NonNull BaseSchedulerProvider schedulerProvider) {
        mTasksRemoteDataSource = checkNotNull(tasksRemoteDataSource);
        mTasksLocalDataSource = checkNotNull(tasksLocalDataSource);
        mBaseSchedulerProvider = checkNotNull(schedulerProvider);
    }

    /**
     * Returns the single instance of this class, creating it if necessary.
     *
     * @param tasksRemoteDataSource the backend data source
     * @param tasksLocalDataSource  the device storage data source
     * @return the {@link TasksRepository} instance
     */
    public static TasksRepository getInstance(@NonNull TasksDataSource tasksRemoteDataSource,
                                              @NonNull TasksDataSource tasksLocalDataSource,
                                              @NonNull BaseSchedulerProvider schedulerProvider) {
        if (INSTANCE == null) {
            INSTANCE = new TasksRepository(tasksRemoteDataSource, tasksLocalDataSource,
                    schedulerProvider);
        }
        return INSTANCE;
    }

    /**
     * Used to force {@link #getInstance(TasksDataSource, TasksDataSource, BaseSchedulerProvider)}
     * to create a new instance next time it's called.
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }

    /**
     * Gets tasks from  local data source (SQLite).
     */
    @Override
    public Observable<List<Task>> getTasks() {
        return mTasksLocalDataSource.getTasks();
    }

    /**
     * Saves a task in the local and then in the remote repository
     *
     * @param task the task to be saved
     * @return a completable that emits when the task was saved or in case of error.
     */
    @NonNull
    @Override
    public Completable saveTask(@NonNull Task task) {
        checkNotNull(task);
        return mTasksLocalDataSource.saveTask(task)
                .andThen(mTasksRemoteDataSource.saveTask(task));
    }

    /**
     * Saves a list of tasks in the local and then in the remote repository
     *
     * @param tasks the tasks to be saved
     * @return a completable that emits when the tasks were saved or in case of error.
     */
    @Override
    public Completable saveTasks(@NonNull List<Task> tasks) {
        checkNotNull(tasks);
        return mTasksLocalDataSource.saveTasks(tasks)
                .andThen(mTasksRemoteDataSource.saveTasks(tasks));
    }

    @Override
    public void completeTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.completeTask(task);
        mTasksLocalDataSource.completeTask(task);
    }

    @Override
    public void completeTask(@NonNull String taskId) {
        checkNotNull(taskId);
        mTasksRemoteDataSource.completeTask(taskId);
        mTasksLocalDataSource.completeTask(taskId);
    }

    @Override
    public void activateTask(@NonNull Task task) {
        checkNotNull(task);
        mTasksRemoteDataSource.activateTask(task);
        mTasksLocalDataSource.activateTask(task);
    }

    @Override
    public void activateTask(@NonNull String taskId) {
        checkNotNull(taskId);
        mTasksRemoteDataSource.activateTask(taskId);
        mTasksLocalDataSource.activateTask(taskId);
    }

    @Override
    public void clearCompletedTasks() {
        mTasksRemoteDataSource.clearCompletedTasks();
        mTasksLocalDataSource.clearCompletedTasks();
    }

    /**
     * Gets tasks from local data source (sqlite) unless the table is new or empty. In that case it
     * uses the network data source. This is done to simplify the sample.
     */
    @Override
    public Observable<Task> getTask(@NonNull final String taskId) {
        checkNotNull(taskId);
        return mTasksLocalDataSource.getTask(taskId);
    }

    @Override
    public void refreshTasks() {
        mTasksRemoteDataSource.getTasks()
                .subscribeOn(mBaseSchedulerProvider.io())
                .subscribe(mTasksLocalDataSource::saveTasks);
    }

    @Override
    public void deleteAllTasks() {
        mTasksRemoteDataSource.deleteAllTasks();
        mTasksLocalDataSource.deleteAllTasks();
    }

    @Override
    public void deleteTask(@NonNull String taskId) {
        mTasksRemoteDataSource.deleteTask(checkNotNull(taskId));
        mTasksLocalDataSource.deleteTask(checkNotNull(taskId));
    }
}
