package com.pcmsolutions.system.tasking;

import com.pcmsolutions.system.ZDisposable;
import com.pcmsolutions.system.delays.Delay;
import com.pcmsolutions.system.threads.Impl_ZThread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: paulmeehan
 * Date: 07-Aug-2004
 * Time: 11:11:15
 */
abstract class Impl_TaskQ <T extends Task> implements ManageableTaskQ<T>, ZDisposable, ThreadAllower, TaskFetcher<T>, TaskInserter<T>, Delay {
    final Object owner;
    final String name;
    final int priority;
    final LinkedList<T> queue = new LinkedList<T>();
    final LinkedList<T> flush_queue = new LinkedList<T>();
    final LinkedList<Runnable> cb_queue = new LinkedList<Runnable>();
    boolean started = false;
    boolean paused = false;

    public Impl_TaskQ(Object owner, String name, int priority) {
        this.owner = owner;
        this.name = name;
        this.priority = priority;
    }

    private TaskThread taskThread;
    private TaskThread callbackThread;

    final TaskThread createTaskThread() {
        return new Impl_TaskThread("TQ: " + name, this) {
            public void runBody() {
                try {
                    List<T> tasks;
                    while (shouldRun) {
                        tasks = null;
                        synchronized (queue) {
                            if (queue.isEmpty())
                                queue.notifyAll(); // in case some threads are waiting on an empty queue
                            while (queue.isEmpty() || paused) {
                                try {
                                    queue.wait();
                                } catch (InterruptedException e) {
                                    if (shouldRun == false)
                                        return;
                                }
                            }
                        }
                        try {
                            beginDelay();
                            blockOnDelay();
                            synchronized (queue) {
                                tasks = fetch(queue);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            runTasks(tasks);
                        }
                    }
                } finally {
                    cancelQueue();
                }
            }
        };
    }

    public List<T> fetch(List<T> q) {
        List<T> ta = new ArrayList<T>();
        ta.add(queue.remove(0));
        return ta;
    }

    public void beginDelay() {

    }

    public void blockOnDelay() {

    }

    final TaskThread createCallbackThread() {
        return new Impl_TaskThread("TQ(callback): " + name, this) {
            public void runBody() {
                while (true) {
                    Runnable r = null;
                    synchronized (cb_queue) {
                        while (cb_queue.isEmpty()) {
                            if (shouldRun == false)
                                return;
                            try {
                                cb_queue.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        r = cb_queue.remove(0);
                    }
                    try {
                        r.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void runTasks(final List<T> tasks) {
        if (tasks != null)
            try {
                try {
                    for (Iterator<T> it = tasks.iterator(); it.hasNext();) {
                        T task = it.next();
                        Exception exception = null;
                        if (task != null)
                            try {
                                task.run();
                            } catch (Exception e) {
                                exception = e;
                                System.out.println("Task = " + task.getName());
                                e.printStackTrace();
                            } finally {
                                synchronized (cb_queue) {
                                    final Exception f_exception = exception;
                                    final T f_task = task;
                                    cb_queue.add(new Runnable() {
                                        public void run() {
                                            f_task.cbFinished(f_exception);
                                        }
                                    });
                                    cb_queue.notifyAll();
                                }
                            }
                    }
                } finally {
                    ((Impl_TaskThread) Thread.currentThread()).performCompletedActions();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private final void cancelQueue() {
        synchronized (queue) {
            Iterator<T> i = queue.iterator();
            while (i.hasNext())
                try {
                    synchronized (cb_queue) {
                        final T t = i.next();
                        cb_queue.add(t::cbCancelled);
                        cb_queue.notifyAll();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            queue.clear();
            flush();
        }
    }

    private final void flush() {
        synchronized (flush_queue) {
            runTasks(flush_queue);
            flush_queue.clear();
        }
    }

    public final void postTask(final T t) throws QueueUnavailableException {
        if (t == null)
            throw new IllegalArgumentException("null task!");
        if (!allowThread())
            throw new QueueUnavailableException(name, "thread not allowed on q");
        synchronized (queue) {
            if (!started)
                throw new QueueUnavailableException(name, "queue not started");
            insertTask(t, queue, name);
            queue.notifyAll();
        }
    }

    public void insertTask(final T t, final List<T> q, String qName) throws QueueUnavailableException {
        if (q.contains(t))
            throw new QueueUnavailableException(qName, "task already posted");
        q.add(t);
    }

    public void stop(boolean flush) {
        synchronized (queue) {
            if (started) {
                paused = false;
                if (flush) {
                    flush_queue.addAll(queue);
                    queue.clear();
                }
                taskThread.stopThreadSafely();
                callbackThread.stopThreadSafely();
                taskThread = null;
                callbackThread = null;
                started = false;
            }
        }
    }

    public void start() {
        synchronized (queue) {
            if (!started) {
                taskThread = createTaskThread();
                callbackThread = createCallbackThread();
                try {
                    taskThread.setPriority(priority);
                    callbackThread.setPriority(priority);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                taskThread.start();
                callbackThread.start();
                started = true;
            }
        }
    }

    public void pause() {
        synchronized (queue) {
            paused = true;
            notifyAll();
        }
    }

    public void resume() {
        synchronized (queue) {
            paused = false;
            notifyAll();
        }
    }

    public void cancel() {
        cancelQueue();
    }

    public Object getOwner() {
        return owner;
    }

    public void waitUntilEmpty() {
        synchronized (queue) {
            while (!queue.isEmpty())
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
    /*
    public void runWhenEmpty(Runnable r) {
        synchronized (queue) {
            while (!queue.isEmpty())
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            r.run();
        }
    }
    */
    public String getName() {
        return name;
    }

    public boolean isReentering() {
        if (Thread.currentThread() == taskThread)
            return true;
        else
            return false;
    }

    public void zDispose() {
        stop(false);
    }

    abstract class Impl_TaskThread extends Impl_ZThread implements TaskThread {
        final ManageableTaskQ associatedQueue;

        public Impl_TaskThread(ThreadGroup group, String name, ManageableTaskQ queue) {
            super(group, name);
            this.associatedQueue = queue;
        }

        public Impl_TaskThread(ManageableTaskQ queue) {
            this.associatedQueue = queue;
        }

        public Impl_TaskThread(String name, ManageableTaskQ queue) {
            super(name);
            this.associatedQueue = queue;
        }

        public TaskQ getAssociatedQueue() {
            return associatedQueue;
        }
    }
}
