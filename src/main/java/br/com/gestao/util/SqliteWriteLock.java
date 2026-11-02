package br.com.gestao.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.locks.ReentrantLock;

public final class SqliteWriteLock {
    private static final ReentrantLock LOCK = new ReentrantLock(true);

    private SqliteWriteLock() {}

    public static void lockUntilTransactionCompletes() {
        LOCK.lock();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            LOCK.unlock();
            throw new IllegalStateException("Operação de escrita precisa estar dentro de uma transação.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                LOCK.unlock();
            }
        });
    }
}
