package com.mes.production.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes writes to the CRDB write node and reads to the CRDB read node.
 * The lookup key is derived from the current transaction's readOnly flag:
 *   readOnly=true  -> READ  (CRDB port 26267 / follower node)
 *   readOnly=false -> WRITE (CRDB port 26257 / leader node)
 *
 * This gives transparent read-write separation for all @Transactional methods
 * (readOnly reads use the read pool, writes use the write pool).
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    public static final String WRITE = "WRITE";
    public static final String READ = "READ";

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? READ : WRITE;
    }
}
