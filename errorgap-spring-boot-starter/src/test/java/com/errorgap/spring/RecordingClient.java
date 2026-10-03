package com.errorgap.spring;

import com.errorgap.ApmTransaction;
import com.errorgap.Client;
import com.errorgap.Configuration;
import com.errorgap.NoticeOptions;
import com.errorgap.TransactionContext;

import java.util.ArrayList;
import java.util.List;

final class RecordingClient extends Client {
    final List<ApmTransaction> transactions = new ArrayList<>();
    final List<Throwable> errors = new ArrayList<>();
    final List<NoticeOptions> options = new ArrayList<>();
    /** The transaction current when each error was reported. */
    final List<String> transactionIds = new ArrayList<>();

    RecordingClient() {
        super(new Configuration()
            .setProjectSlug("test")
            .setApmEnabled(true)
            .setAsync(false));
    }

    @Override
    public Result notifyTransaction(ApmTransaction transaction) {
        transactions.add(transaction);
        return new Result(201, "{}", null, false);
    }

    @Override
    public Result notify(Throwable throwable, NoticeOptions noticeOptions, boolean sync) {
        errors.add(throwable);
        options.add(noticeOptions);
        transactionIds.add(TransactionContext.current());
        return new Result(201, "{}", null, false);
    }
}
