package stockstream.logic;


import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;


@Slf4j
public class PubSub {
    private static final int PUBSUB_POOL_SIZE = Integer.valueOf(System.getenv().getOrDefault("PUBSUB_POOL_SIZE", "32"));

    private final ExecutorService executorService = Executors.newFixedThreadPool(PUBSUB_POOL_SIZE);

    private Map<Class<?>, Set<Function>> functionSubscriptions = Collections.synchronizedMap(new HashMap<>());
    private Map<Class<?>, Set<Runnable>> runnableSubscriptions = Collections.synchronizedMap(new HashMap<>());

    public void publishAsync(final Class<?> type, final Object object) {
        new Thread(() -> publishClassType(type, object)).start();
    }

    public void publishClassType(final Class<?> type, final Object object) {

        final Set<Function> functions = functionSubscriptions.getOrDefault(type, Collections.emptySet());
        final Set<Runnable> runnables = runnableSubscriptions.getOrDefault(type, Collections.emptySet());

        final int jobCount = Math.min(PUBSUB_POOL_SIZE, functions.size() + runnables.size());

        final List<Future<?>> executorFutures = new ArrayList<>(jobCount);

        final ConcurrentLinkedQueue<Function> functionQueue = new ConcurrentLinkedQueue<>(functions);
        final ConcurrentLinkedQueue<Runnable> runnableQueue = new ConcurrentLinkedQueue<>(runnables);

        for (int i = 0; i < jobCount; ++i) {
            executorFutures.add(executorService.submit(() -> {
                while (!functionQueue.isEmpty()) {
                    final Function function = functionQueue.poll();
                    if (function == null) {
                        continue;
                    }
                    try {
                        function.apply(object);
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
                while (!runnableQueue.isEmpty()) {
                    final Runnable runnable = runnableQueue.poll();
                    if (runnable == null) {
                        continue;
                    }
                    try {
                        runnable.run();
                    } catch (final Exception e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            }));
        }

        try {
            for (final Future<?> demandScoreFuture : executorFutures) {
                demandScoreFuture.get();
            }
        } catch (final CancellationException | InterruptedException | ExecutionException e) {
            log.warn(e.getMessage(), e);
        }

    }

    public <T> void subscribeFunctionToClassType(final Function<T, Void> onReceive, final Class<?> type) {
        functionSubscriptions.computeIfAbsent(type, set -> Collections.synchronizedSet(new HashSet<>())).add(onReceive);
    }

    public void subscribeRunnableToClassType(final Runnable runnable, final Class<?> type) {
        runnableSubscriptions.computeIfAbsent(type, set -> Collections.synchronizedSet(new HashSet<>())).add(runnable);
    }

}
