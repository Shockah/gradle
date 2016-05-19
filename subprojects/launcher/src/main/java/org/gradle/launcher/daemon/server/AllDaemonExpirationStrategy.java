/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.launcher.daemon.server;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Expires the daemon only if all children would expire the daemon.
 */
public class AllDaemonExpirationStrategy implements DaemonExpirationStrategy {
    private Iterable<DaemonExpirationStrategy> expirationStrategies;

    public AllDaemonExpirationStrategy(List<DaemonExpirationStrategy> expirationStrategies) {
        this.expirationStrategies = expirationStrategies;
    }

    public DaemonExpirationResult checkExpiration(Daemon daemon) {
        // If no expiration strategies exist, the daemon will not expire.
        DaemonExpirationResult expirationResult = DaemonExpirationResult.DO_NOT_EXPIRE;
        List<String> reasons = Lists.newArrayList();
        boolean terminated = true;
        boolean immediate = false;

        for (DaemonExpirationStrategy expirationStrategy : expirationStrategies) {
            // If any of the child strategies don't expire the daemon, the daemon will not expire.
            // Otherwise, the daemon will expire and aggregate the reasons together.
            expirationResult = expirationStrategy.checkExpiration(daemon);

            if (!expirationResult.isExpired()) {
                return DaemonExpirationResult.DO_NOT_EXPIRE;
            } else {
                immediate = immediate || expirationResult.isImmediate();
                terminated = terminated && expirationResult.isTerminated();
                reasons.add(expirationResult.getReason());
            }
        }

        if (!expirationResult.isExpired()) {
            return expirationResult;
        } else {
            return new DaemonExpirationResult(true, immediate, terminated, Joiner.on(" and ").skipNulls().join(reasons));
        }
    }
}
