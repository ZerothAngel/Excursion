/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.Excursion;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionStrategy;

public class TransactionWrapperExcursionDao implements ExcursionDao {

    private final ExcursionDao delegate;
    
    private final TransactionStrategy transactionStrategy;

    public TransactionWrapperExcursionDao(ExcursionDao delegate, TransactionStrategy transactionStrategy) {
        this.delegate = delegate;
        this.transactionStrategy = transactionStrategy;
    }

    @Override
    public void saveLocation(final Player player, final String group, final Location location) {
        transactionStrategy.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult() throws Exception {
                delegate.saveLocation(player, group, location);
            }
        });
    }

    @Override
    public Location loadLocation(final Player player, final String group) {
        return transactionStrategy.execute(new TransactionCallback<Location>() {
            @Override
            public Location doInTransaction() throws Exception {
                return delegate.loadLocation(player, group);
            }
        });
    }

}
